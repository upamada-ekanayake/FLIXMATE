package com.flixmate.core.controller;

import com.flixmate.core.dto.BookingRequest;
import com.flixmate.core.dto.BookingResponse;
import com.flixmate.core.model.Booking;
import com.flixmate.core.model.BookedSeat;
import com.flixmate.core.model.User;
import com.flixmate.core.repository.BookingRepository;
import com.flixmate.core.repository.BookedSeatRepository;
import com.flixmate.core.service.BookingService;
import com.flixmate.core.service.PDFGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final PDFGeneratorService pdfGeneratorService;

    @PostMapping("/hold")
    public ResponseEntity<BookingResponse> holdSeats(@RequestBody BookingRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(bookingService.holdSeats(user.getId(), request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable UUID id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (!booking.getUser().getId().equals(user.getId()) && 
            !user.getRole().name().equals("ROLE_SUPER_ADMIN") && 
            !user.getRole().name().equals("ROLE_THEATER_MANAGER")) {
            throw new IllegalStateException("You are not authorized to confirm this booking.");
        }

        return ResponseEntity.ok(bookingService.confirmBooking(id));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Booking>> getBookingHistory() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadTicketPDF(@PathVariable UUID id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (!booking.getUser().getId().equals(user.getId()) && 
            !user.getRole().name().equals("ROLE_SUPER_ADMIN") && 
            !user.getRole().name().equals("ROLE_THEATER_MANAGER")) {
            throw new IllegalStateException("You are not authorized to access this ticket PDF.");
        }

        List<BookedSeat> occupied = bookedSeatRepository.findOccupiedSeatsByShowtime(booking.getShowtime().getId());
        List<BookedSeat> seatsForBooking = occupied.stream()
                .filter(bs -> bs.getBooking().getId().equals(id))
                .toList();

        byte[] pdfBytes = pdfGeneratorService.generateTicketPDF(booking, seatsForBooking);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=FlixMate_Ticket_" + id.toString().substring(0, 8) + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
