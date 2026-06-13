package com.flixmate.core.config;

import com.flixmate.core.model.*;
import com.flixmate.core.repository.*;
import com.flixmate.core.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final ReviewRepository reviewRepository;
    private final MovieService movieService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping seeder.");
            return;
        }

        log.info("Starting database seeding process...");

        // 1. Seed Users
        User admin = User.builder()
                .email("admin@flixmate.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("FlixMate")
                .lastName("Admin")
                .role(Role.ROLE_SUPER_ADMIN)
                .build();
        userRepository.save(admin);

        User customer = User.builder()
                .email("user@flixmate.com")
                .password(passwordEncoder.encode("user123"))
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(customer);
        log.info("Admin (admin@flixmate.com) and User (user@flixmate.com) accounts created.");

        // 2. Seed Movies (Seeds Interstellar, Inception, Dark Knight, Gladiator)
        List<Movie> movies = movieService.getAllMovies();
        log.info("Seeded {} movies.", movies.size());

        // 3. Seed Theater
        Theater theater = Theater.builder()
                .name("IMAX Cineplex Metrotown")
                .city("Vancouver")
                .address("4820 Kingsway, Burnaby")
                .totalScreens(1)
                .build();
        theater = theaterRepository.save(theater);

        // 4. Seed Screen
        Screen screen = Screen.builder()
                .theater(theater)
                .name("Screen 1 (IMAX 3D)")
                .totalSeats(40)
                .build();
        screen = screenRepository.save(screen);

        // 5. Seed Seats (A-D, 1-10)
        List<Seat> seats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D"};
        for (String row : rows) {
            for (int i = 1; i <= 10; i++) {
                SeatType type = SeatType.STANDARD;
                if ("C".equals(row)) type = SeatType.PREMIUM;
                if ("D".equals(row)) type = SeatType.VIP;

                Seat seat = Seat.builder()
                        .screen(screen)
                        .rowName(row)
                        .seatNumber(i)
                        .type(type)
                        .build();
                seats.add(seat);
            }
        }
        seats = seatRepository.saveAll(seats);
        log.info("Seeded 40 seats in Screen 1 (Rows A-D).");

        // 6. Seed Showtimes
        LocalDateTime today6PM = LocalDateTime.of(LocalDate.now(), LocalTime.of(18, 0));
        LocalDateTime today9PM = LocalDateTime.of(LocalDate.now(), LocalTime.of(21, 0));
        LocalDateTime tomorrow3PM = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(15, 0));

        Showtime s1 = Showtime.builder()
                .movie(movies.get(0)) // Interstellar
                .screen(screen)
                .startTime(today6PM)
                .endTime(today6PM.plusMinutes(movies.get(0).getDurationMinutes()))
                .basePrice(new BigDecimal("15.00"))
                .currentPrice(new BigDecimal("15.00"))
                .build();
        
        Showtime s2 = Showtime.builder()
                .movie(movies.get(1)) // Inception
                .screen(screen)
                .startTime(today9PM)
                .endTime(today9PM.plusMinutes(movies.get(1).getDurationMinutes()))
                .basePrice(new BigDecimal("14.00"))
                .currentPrice(new BigDecimal("14.00"))
                .build();

        Showtime s3 = Showtime.builder()
                .movie(movies.get(2)) // The Dark Knight
                .screen(screen)
                .startTime(tomorrow3PM)
                .endTime(tomorrow3PM.plusMinutes(movies.get(2).getDurationMinutes()))
                .basePrice(new BigDecimal("13.00"))
                .currentPrice(new BigDecimal("13.00"))
                .build();

        showtimeRepository.saveAll(List.of(s1, s2, s3));
        log.info("Seeded 3 movie showtimes.");

        // 7. Seed Reviews
        Review r1 = Review.builder()
                .movie(movies.get(0)) // Interstellar
                .user(customer)
                .rating(5)
                .comment("Absolute masterpiece! Hans Zimmer's soundtrack is out of this world.")
                .sentiment(SentimentType.POSITIVE)
                .build();

        Review r2 = Review.builder()
                .movie(movies.get(0))
                .user(admin)
                .rating(2)
                .comment("Way too long and confusing. The sound design was too loud in the cinema.")
                .sentiment(SentimentType.NEGATIVE)
                .build();

        Review r3 = Review.builder()
                .movie(movies.get(1)) // Inception
                .user(customer)
                .rating(4)
                .comment("Incredibly smart screenplay, great acting by Leo. Loved the spinning top ending.")
                .sentiment(SentimentType.POSITIVE)
                .build();

        reviewRepository.saveAll(List.of(r1, r2, r3));
        log.info("Seeded 3 reviews with sentiment analysis.");

        // 8. Seed Historical Bookings (For analytics charts: Weeks 0, -1, -2, -3)
        List<Booking> historicalBookings = new ArrayList<>();
        List<BookedSeat> historicalSeats = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Week -3
        Booking b1 = Booking.builder()
                .user(customer)
                .showtime(s1)
                .status(BookingStatus.CONFIRMED)
                .totalPrice(new BigDecimal("45.00"))
                .paymentIntentId("pi_hist_1")
                .createdAt(now.minusWeeks(3).minusDays(2))
                .build();
        historicalBookings.add(b1);

        // Week -2
        Booking b2 = Booking.builder()
                .user(customer)
                .showtime(s2)
                .status(BookingStatus.CONFIRMED)
                .totalPrice(new BigDecimal("56.00"))
                .paymentIntentId("pi_hist_2")
                .createdAt(now.minusWeeks(2).minusDays(4))
                .build();
        historicalBookings.add(b2);

        // Week -1
        Booking b3 = Booking.builder()
                .user(customer)
                .showtime(s1)
                .status(BookingStatus.CONFIRMED)
                .totalPrice(new BigDecimal("75.00"))
                .paymentIntentId("pi_hist_3")
                .createdAt(now.minusWeeks(1).minusDays(1))
                .build();
        historicalBookings.add(b3);

        // Week -0
        Booking b4 = Booking.builder()
                .user(customer)
                .showtime(s3)
                .status(BookingStatus.CONFIRMED)
                .totalPrice(new BigDecimal("30.00"))
                .paymentIntentId("pi_hist_4")
                .createdAt(now.minusDays(1))
                .build();
        historicalBookings.add(b4);

        bookingRepository.saveAll(historicalBookings);

        // Associate BookedSeats to bookings
        historicalSeats.add(BookedSeat.builder().booking(b1).seat(seats.get(0)).status(BookedSeatStatus.BOOKED).build());
        historicalSeats.add(BookedSeat.builder().booking(b1).seat(seats.get(1)).status(BookedSeatStatus.BOOKED).build());
        historicalSeats.add(BookedSeat.builder().booking(b2).seat(seats.get(5)).status(BookedSeatStatus.BOOKED).build());
        historicalSeats.add(BookedSeat.builder().booking(b3).seat(seats.get(10)).status(BookedSeatStatus.BOOKED).build());
        historicalSeats.add(BookedSeat.builder().booking(b4).seat(seats.get(15)).status(BookedSeatStatus.BOOKED).build());

        bookedSeatRepository.saveAll(historicalSeats);
        log.info("Seeded historical bookings for Admin Dashboard charts.");

        log.info("Database seeding successfully completed!");
    }
}
