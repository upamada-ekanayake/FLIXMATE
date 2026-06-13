package com.flixmate.core.service;

import com.flixmate.core.dto.BookingRequest;
import com.flixmate.core.dto.BookingResponse;
import com.flixmate.core.dto.SeatStatusDto;
import com.flixmate.core.model.*;
import com.flixmate.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    
    private final PaymentService paymentService;
    private final QRGeneratorService qrGeneratorService;
    private final PDFGeneratorService pdfGeneratorService;
    private final EmailSenderService emailSenderService;
    
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Retrieve all seats in a screen and mark their status based on active bookings & holds.
     */
    public List<SeatStatusDto> getSeatStatuses(UUID showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Showtime not found."));

        List<Seat> allSeats = seatRepository.findByScreenId(showtime.getScreen().getId());
        List<BookedSeat> occupied = bookedSeatRepository.findOccupiedSeatsByShowtime(showtimeId);
        
        Map<UUID, BookedSeat> occupiedMap = occupied.stream()
                .collect(Collectors.toMap(bs -> bs.getSeat().getId(), bs -> bs));

        return allSeats.stream().map(seat -> {
            String status = "AVAILABLE";
            BookedSeat bs = occupiedMap.get(seat.getId());
            if (bs != null) {
                status = bs.getStatus().name();
            }
            return SeatStatusDto.builder()
                    .seatId(seat.getId())
                    .rowName(seat.getRowName())
                    .seatNumber(seat.getSeatNumber())
                    .type(seat.getType().name())
                    .status(status)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Atomically place a lock/hold on seats for 10 minutes.
     */
    @Transactional
    public BookingResponse holdSeats(UUID userId, BookingRequest request) {
        log.info("Attempting to hold seats {} for showtime {}", request.getSeatIds(), request.getShowtimeId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        
        Showtime showtime = showtimeRepository.findByIdWithWriteLock(request.getShowtimeId())
                .orElseThrow(() -> new IllegalArgumentException("Showtime not found."));

        // Verify that none of the requested seats are already reserved or held
        List<BookedSeat> occupied = bookedSeatRepository.findOccupiedSeatsByShowtime(request.getShowtimeId());
        Set<UUID> occupiedSeatIds = occupied.stream()
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toSet());

        for (UUID requestedSeatId : request.getSeatIds()) {
            if (occupiedSeatIds.contains(requestedSeatId)) {
                throw new IllegalStateException("One or more selected seats are already locked or booked.");
            }
        }

        // Calculate pricing
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        BigDecimal total = BigDecimal.ZERO;
        for (Seat seat : seats) {
            BigDecimal price = showtime.getCurrentPrice();
            if (seat.getType() == SeatType.PREMIUM) {
                price = price.multiply(new BigDecimal("1.2")); // +20%
            } else if (seat.getType() == SeatType.VIP) {
                price = price.multiply(new BigDecimal("1.5")); // +50%
            }
            total = total.add(price);
        }

        // Create Booking (PENDING state)
        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .status(BookingStatus.PENDING)
                .totalPrice(total)
                .build();
        
        booking = bookingRepository.save(booking);

        // Place Holds on Seats
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
        List<BookedSeat> holds = new ArrayList<>();
        for (Seat seat : seats) {
            BookedSeat hold = BookedSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .status(BookedSeatStatus.HOLD)
                    .holdExpiresAt(expiry)
                    .build();
            holds.add(hold);
        }
        bookedSeatRepository.saveAll(holds);

        // Broadcast updated seat states to all subscribers
        broadcastSeatStatus(request.getShowtimeId());

        // Create Stripe/Mock Payment Intent
        String paymentIntentId = paymentService.createPaymentIntent(total, "USD");
        booking.setPaymentIntentId(paymentIntentId);
        bookingRepository.save(booking);

        List<String> labels = seats.stream()
                .map(s -> s.getRowName() + "-" + s.getSeatNumber())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .showtimeId(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .theaterName(showtime.getScreen().getTheater().getName())
                .screenName(showtime.getScreen().getName())
                .startTime(showtime.getStartTime().toString())
                .totalPrice(total)
                .status(booking.getStatus().name())
                .seatLabels(labels)
                .paymentIntentId(paymentIntentId)
                .build();
    }

    /**
     * Confirm a ticket order after checking payment validity.
     */
    @Transactional
    public BookingResponse confirmBooking(UUID bookingId) {
        log.info("Confirming booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is already finalized or cancelled.");
        }

        // Verify simulated checkout
        boolean paymentSuccess = paymentService.confirmPayment(booking.getPaymentIntentId());
        if (!paymentSuccess) {
            throw new IllegalStateException("Payment verification failed.");
        }

        // Update status to CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Finalize Seat state to BOOKED
        List<BookedSeat> occupied = bookedSeatRepository.findOccupiedSeatsByShowtime(booking.getShowtime().getId());
        List<BookedSeat> holdsForThisBooking = occupied.stream()
                .filter(bs -> bs.getBooking().getId().equals(bookingId))
                .toList();

        for (BookedSeat bs : holdsForThisBooking) {
            bs.setStatus(BookedSeatStatus.BOOKED);
            bs.setHoldExpiresAt(null);
        }
        bookedSeatRepository.saveAll(holdsForThisBooking);

        // Broadcast seat grid changes via Websocket
        broadcastSeatStatus(booking.getShowtime().getId());

        // Generate QR code base64
        String qrBase64 = qrGeneratorService.generateQRCodeBase64(booking.getId().toString(), 150, 150);

        // Generate PDF ticket receipt
        byte[] pdfTicket = pdfGeneratorService.generateTicketPDF(booking, holdsForThisBooking);

        // Send Email notification
        emailSenderService.sendBookingConfirmationEmail(booking.getUser().getEmail(), booking, pdfTicket);

        List<String> labels = holdsForThisBooking.stream()
                .map(h -> h.getSeat().getRowName() + "-" + h.getSeat().getSeatNumber())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .showtimeId(booking.getShowtime().getId())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .theaterName(booking.getShowtime().getScreen().getTheater().getName())
                .screenName(booking.getShowtime().getScreen().getName())
                .startTime(booking.getShowtime().getStartTime().toString())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus().name())
                .seatLabels(labels)
                .qrCodeBase64(qrBase64)
                .paymentIntentId(booking.getPaymentIntentId())
                .build();
    }

    /**
     * Periodically release expired holds (Runs every 60 seconds).
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<BookedSeat> expiredHolds = bookedSeatRepository.findByStatusAndHoldExpiresAtBefore(BookedSeatStatus.HOLD, now);
        
        if (expiredHolds.isEmpty()) {
            return;
        }

        log.info("Releasing {} expired seat holds.", expiredHolds.size());
        
        // Find affected showtimes to broadcast updates
        Set<UUID> showtimeIds = expiredHolds.stream()
                .map(eh -> eh.getBooking().getShowtime().getId())
                .collect(Collectors.toSet());

        // Remove expired seat locks
        bookedSeatRepository.deleteAll(expiredHolds);

        // Transition affected bookings that remained pending to CANCELLED
        Set<UUID> bookingIds = expiredHolds.stream()
                .map(eh -> eh.getBooking().getId())
                .collect(Collectors.toSet());

        for (UUID bId : bookingIds) {
            Booking booking = bookingRepository.findById(bId).orElse(null);
            if (booking != null && booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            }
        }

        // Broadcast to each showtime topic
        for (UUID sId : showtimeIds) {
            broadcastSeatStatus(sId);
        }
    }

    private void broadcastSeatStatus(UUID showtimeId) {
        try {
            List<SeatStatusDto> status = getSeatStatuses(showtimeId);
            messagingTemplate.convertAndSend("/topic/showtime/" + showtimeId, status);
        } catch (Exception e) {
            log.error("Failed to broadcast seat updates via WebSocket for showtime {}", showtimeId, e);
        }
    }
}
