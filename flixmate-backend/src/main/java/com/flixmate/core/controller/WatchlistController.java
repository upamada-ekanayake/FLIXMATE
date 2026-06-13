package com.flixmate.core.controller;

import com.flixmate.core.model.User;
import com.flixmate.core.model.Watchlist;
import com.flixmate.core.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/watchlists")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<List<Watchlist>> getWatchlist() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(watchlistService.getWatchlist(user.getId()));
    }

    @PostMapping("/{movieId}")
    public ResponseEntity<Watchlist> addToWatchlist(@PathVariable UUID movieId) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(watchlistService.addToWatchlist(user.getId(), movieId));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable UUID movieId) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        watchlistService.removeFromWatchlist(user.getId(), movieId);
        return ResponseEntity.ok().build();
    }
}
