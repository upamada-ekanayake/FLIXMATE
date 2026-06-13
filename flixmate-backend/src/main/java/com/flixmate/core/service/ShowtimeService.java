package com.flixmate.core.service;

import com.flixmate.core.model.BookedSeat;
import com.flixmate.core.model.Showtime;
import com.flixmate.core.repository.BookedSeatRepository;
import com.flixmate.core.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final AIService aiService;

    public List<Showtime> getAllShowtimes() {
        List<Showtime> showtimes = showtimeRepository.findAll();
        showtimes.forEach(this::updateDynamicPrice);
        return showtimes;
    }

    public List<Showtime> getShowtimesByMovie(UUID movieId) {
        List<Showtime> showtimes = showtimeRepository.findByMovieId(movieId);
        showtimes.forEach(this::updateDynamicPrice);
        return showtimes;
    }

    public List<Showtime> getShowtimesByMovieAndTheater(UUID movieId, UUID theaterId) {
        List<Showtime> showtimes = showtimeRepository.findByMovieAndTheater(movieId, theaterId);
        showtimes.forEach(this::updateDynamicPrice);
        return showtimes;
    }

    public Showtime getShowtimeById(UUID id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Showtime not found."));
        updateDynamicPrice(showtime);
        return showtime;
    }

    public Showtime saveShowtime(Showtime showtime) {
        showtime.setCurrentPrice(showtime.getBasePrice()); // Default
        return showtimeRepository.save(showtime);
    }

    public void deleteShowtime(UUID id) {
        showtimeRepository.deleteById(id);
    }

    /**
     * Compute current occupancy and query AI service to adjust the ticket price
     */
    private void updateDynamicPrice(Showtime showtime) {
        try {
            int totalSeats = showtime.getScreen().getTotalSeats();
            if (totalSeats <= 0) {
                showtime.setCurrentPrice(showtime.getBasePrice());
                return;
            }

            List<BookedSeat> occupied = bookedSeatRepository.findOccupiedSeatsByShowtime(showtime.getId());
            double occupancyRate = (double) occupied.size() / totalSeats;

            BigDecimal dynamicPrice = aiService.calculateDynamicPrice(showtime, occupancyRate);
            showtime.setCurrentPrice(dynamicPrice);
            
            // Save state back to database
            showtimeRepository.save(showtime);
        } catch (Exception e) {
            log.error("Could not calculate dynamic pricing for showtime {}", showtime.getId(), e);
        }
    }
}
