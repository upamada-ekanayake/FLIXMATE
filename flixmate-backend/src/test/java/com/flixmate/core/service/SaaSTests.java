package com.flixmate.core.service;

import com.flixmate.core.dto.BookingRequest;
import com.flixmate.core.dto.BookingResponse;
import com.flixmate.core.model.*;
import com.flixmate.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SaaSTests {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TheaterRepository theaterRepository;
    @Autowired
    private ScreenRepository screenRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private WatchlistRepository watchlistRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookedSeatRepository bookedSeatRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private BookingService bookingService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private WatchlistService watchlistService;
    @Autowired
    private NotificationService notificationService;

    private User user;
    private Movie movie;
    private Showtime showtime;
    private Seat seat;

    @BeforeEach
    void setUp() {
        // Clear repositories to prevent test interference
        chatHistoryRepository.deleteAll();
        reviewRepository.deleteAll();
        watchlistRepository.deleteAll();
        notificationRepository.deleteAll();
        bookedSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        showtimeRepository.deleteAll();
        seatRepository.deleteAll();
        screenRepository.deleteAll();
        theaterRepository.deleteAll();
        movieRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();
        auditLogRepository.deleteAll();

        // 1. Setup User
        user = User.builder()
                .email("saas_user@flixmate.com")
                .password("password")
                .firstName("SaaS")
                .lastName("Customer")
                .role(Role.ROLE_USER)
                .loyaltyPoints(100) // 100 points initially = $10 discount
                .build();
        user = userRepository.save(user);

        // 2. Setup Theater, Screen, Seat
        Theater theater = Theater.builder()
                .name("SaaS multiplex")
                .city("New York")
                .address("123 SaaS Way")
                .totalScreens(1)
                .build();
        theater = theaterRepository.save(theater);

        Screen screen = Screen.builder()
                .theater(theater)
                .name("IMAX Screen 1")
                .totalSeats(10)
                .rowsCount(2)
                .colsCount(5)
                .build();
        screen = screenRepository.save(screen);

        seat = Seat.builder()
                .screen(screen)
                .rowName("A")
                .seatNumber(1)
                .type(SeatType.STANDARD)
                .build();
        seat = seatRepository.save(seat);

        // 3. Setup Movie
        movie = Movie.builder()
                .title("SaaS Blockbuster")
                .description("Action film")
                .genre("Action")
                .durationMinutes(120)
                .releaseDate(LocalDate.now())
                .averageRating(8.5)
                .tmdbId("saas-test-99")
                .build();
        movie = movieRepository.save(movie);

        // 4. Setup Showtime
        showtime = Showtime.builder()
                .movie(movie)
                .screen(screen)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(4))
                .basePrice(new BigDecimal("20.00"))
                .currentPrice(new BigDecimal("20.00"))
                .build();
        showtime = showtimeRepository.save(showtime);
    }

    @Test
    void testCouponCreationAndValidation() {
        Coupon newCoupon = Coupon.builder()
                .code("PROMO15")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("15.00"))
                .minBookingAmount(new BigDecimal("10.00"))
                .expiryDate(LocalDateTime.now().plusDays(5))
                .build();

        Coupon saved = couponService.createCoupon(newCoupon);
        assertNotNull(saved.getId());

        Coupon validated = couponService.validateCoupon("PROMO15", new BigDecimal("20.00"));
        assertEquals("PROMO15", validated.getCode());
        assertEquals("PERCENTAGE", validated.getDiscountType());
    }

    @Test
    void testLoyaltyEarningAndRedemption() {
        // Hold seat with 50 loyalty points redeemed (value $5.00)
        BookingRequest request = new BookingRequest(showtime.getId(), List.of(seat.getId()), null, 50);
        BookingResponse response = bookingService.holdSeats(user.getId(), request);

        assertNotNull(response.getBookingId());
        // Base price $20.00 - $5.00 (50 points / 10) = $15.00
        assertEquals(0, response.getTotalPrice().compareTo(new BigDecimal("15.00")));

        // Confirm booking
        BookingResponse confirmed = bookingService.confirmBooking(response.getBookingId());
        assertEquals("CONFIRMED", confirmed.getStatus());

        // Check user points: initial 100 - 50 (redeemed) + 15 (earned on $15.00 checkout) = 65
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(65, updatedUser.getLoyaltyPoints());

        // Check that an in-app notification was generated
        List<Notification> notifications = notificationService.getNotificationsForUser(user.getId());
        assertFalse(notifications.isEmpty());
        assertEquals("Booking Confirmed!", notifications.get(0).getTitle());

        // Check that an audit log entry was created
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();
        assertFalse(logs.isEmpty());
        assertEquals("CONFIRM_BOOKING", logs.get(0).getAction());
    }

    @Test
    void testWatchlistManagement() {
        Watchlist added = watchlistService.addToWatchlist(user.getId(), movie.getId());
        assertNotNull(added.getId());

        List<Watchlist> list = watchlistService.getWatchlist(user.getId());
        assertEquals(1, list.size());
        assertEquals(movie.getId(), list.get(0).getMovie().getId());

        watchlistService.removeFromWatchlist(user.getId(), movie.getId());
        List<Watchlist> emptyList = watchlistService.getWatchlist(user.getId());
        assertTrue(emptyList.isEmpty());
    }
}
