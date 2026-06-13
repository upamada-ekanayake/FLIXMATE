package com.flixmate.core.service;

import com.flixmate.core.dto.BookingRequest;
import com.flixmate.core.model.*;
import com.flixmate.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private BookedSeatRepository bookedSeatRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BookingService bookingService;

    private UUID userId;
    private UUID showtimeId;
    private UUID seatId;
    private User user;
    private Showtime showtime;
    private Seat seat;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        showtimeId = UUID.randomUUID();
        seatId = UUID.randomUUID();

        user = User.builder().id(userId).email("test@test.com").build();
        showtime = Showtime.builder()
                .id(showtimeId)
                .basePrice(BigDecimal.TEN)
                .currentPrice(BigDecimal.TEN)
                .screen(Screen.builder().id(UUID.randomUUID()).build())
                .build();
        seat = Seat.builder().id(seatId).type(SeatType.STANDARD).build();
    }

    @Test
    void testHoldSeatsThrowsExceptionWhenSeatAlreadyOccupied() {
        // Arrange
        BookingRequest request = new BookingRequest(showtimeId, List.of(seatId), null, 0);
        BookedSeat occupiedSeat = BookedSeat.builder()
                .seat(seat)
                .status(BookedSeatStatus.BOOKED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(showtimeRepository.findByIdWithWriteLock(showtimeId)).thenReturn(Optional.of(showtime));
        // Mock that the seat is already occupied
        when(bookedSeatRepository.findOccupiedSeatsByShowtime(showtimeId)).thenReturn(List.of(occupiedSeat));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            bookingService.holdSeats(userId, request);
        });
    }
}
