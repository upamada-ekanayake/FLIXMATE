package com.flixmate.core.controller;

import com.flixmate.core.dto.AdminAnalyticsDto;
import com.flixmate.core.model.*;
import com.flixmate.core.repository.*;
import com.flixmate.core.service.AIService;
import com.flixmate.core.service.MovieService;
import com.flixmate.core.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final BookingRepository bookingRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final ReviewRepository reviewRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;

    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final AIService aiService;

    @PostMapping("/movies/sync")
    public ResponseEntity<Movie> syncMovie(@RequestParam String tmdbId) {
        return ResponseEntity.ok(movieService.syncMovieFromTmdb(tmdbId));
    }

    @PostMapping("/showtimes")
    public ResponseEntity<Showtime> createShowtime(@RequestBody Showtime showtime) {
        return ResponseEntity.ok(showtimeService.saveShowtime(showtime));
    }

    @PostMapping("/theaters")
    @Transactional
    public ResponseEntity<Theater> createTheaterWithSeats(@RequestBody Theater theater) {
        // Save Theater
        theater.setTotalScreens(1); // Set Default
        Theater savedTheater = theaterRepository.save(theater);

        // Create Default Screen
        Screen screen = Screen.builder()
                .theater(savedTheater)
                .name("Screen IMAX 3D")
                .totalSeats(40)
                .build();
        screen = screenRepository.save(screen);

        // Populate Seats (4 rows A-D, 10 seats each)
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
        seatRepository.saveAll(seats);

        return ResponseEntity.ok(savedTheater);
    }

    @GetMapping("/analytics")
    public ResponseEntity<AdminAnalyticsDto> getAnalytics() {
        List<Booking> bookings = bookingRepository.findAll();
        
        // Calculate Total Revenue
        BigDecimal totalRevenue = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Occupied Tickets
        long totalTicketsSold = bookedSeatRepository.findAll().stream()
                .filter(bs -> bs.getStatus() == BookedSeatStatus.BOOKED)
                .count();

        // Calculate Average Occupancy
        List<Showtime> showtimes = showtimeRepository.findAll();
        double totalOccupancyRateSum = 0;
        for (Showtime s : showtimes) {
            int totalSeats = s.getScreen().getTotalSeats();
            if (totalSeats > 0) {
                long occupied = bookedSeatRepository.findOccupiedSeatsByShowtime(s.getId()).size();
                totalOccupancyRateSum += ((double) occupied / totalSeats);
            }
        }
        double avgOccupancy = showtimes.isEmpty() ? 0 : (totalOccupancyRateSum / showtimes.size()) * 100.0;

        // Compile Sentiment stats
        List<Review> reviews = reviewRepository.findAll();
        Map<String, Long> sentiments = new HashMap<>();
        sentiments.put("POSITIVE", 0L);
        sentiments.put("NEUTRAL", 0L);
        sentiments.put("NEGATIVE", 0L);
        for (Review r : reviews) {
            if (r.getSentiment() == null) {
                r.setSentiment(aiService.analyzeSentiment(r.getComment()));
                reviewRepository.save(r);
            }
            String sName = r.getSentiment().name();
            sentiments.put(sName, sentiments.getOrDefault(sName, 0L) + 1);
        }

        // Compile weekly history for predictions (Last 4 weeks)
        Map<String, BigDecimal> weeklyRevenue = new LinkedHashMap<>();
        List<BigDecimal> weeklyList = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        for (int i = 3; i >= 0; i--) {
            LocalDateTime start = now.minusWeeks(i + 1);
            LocalDateTime end = now.minusWeeks(i);
            
            BigDecimal weekSum = bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED &&
                            b.getCreatedAt().isAfter(start) && b.getCreatedAt().isBefore(end))
                    .map(Booking::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String weekLabel = "Week -" + i;
            weeklyRevenue.put(weekLabel, weekSum);
            weeklyList.add(weekSum);
        }

        // Feed history to AI Regression forecasting engine
        BigDecimal predictedRevenue = aiService.predictRevenueNextMonth(weeklyList);

        AdminAnalyticsDto dto = AdminAnalyticsDto.builder()
                .totalRevenue(totalRevenue)
                .predictedRevenueNextMonth(predictedRevenue)
                .averageOccupancyRate(avgOccupancy)
                .totalTicketsSold(totalTicketsSold)
                .reviewSentimentCounts(sentiments)
                .weeklyRevenueHistory(weeklyRevenue)
                .build();

        return ResponseEntity.ok(dto);
    }
}
