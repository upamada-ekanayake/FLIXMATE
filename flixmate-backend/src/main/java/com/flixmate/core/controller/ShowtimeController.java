package com.flixmate.core.controller;

import com.flixmate.core.dto.SeatStatusDto;
import com.flixmate.core.model.Showtime;
import com.flixmate.core.service.BookingService;
import com.flixmate.core.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;
    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<Showtime>> getAllShowtimes() {
        return ResponseEntity.ok(showtimeService.getAllShowtimes());
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<Showtime>> getShowtimesByMovie(@PathVariable UUID movieId) {
        return ResponseEntity.ok(showtimeService.getShowtimesByMovie(movieId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Showtime> getShowtimeById(@PathVariable UUID id) {
        return ResponseEntity.ok(showtimeService.getShowtimeById(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatStatusDto>> getSeatStatuses(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getSeatStatuses(id));
    }
}
