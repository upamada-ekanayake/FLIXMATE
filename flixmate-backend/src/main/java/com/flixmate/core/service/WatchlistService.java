package com.flixmate.core.service;

import com.flixmate.core.model.Movie;
import com.flixmate.core.model.User;
import com.flixmate.core.model.Watchlist;
import com.flixmate.core.repository.MovieRepository;
import com.flixmate.core.repository.UserRepository;
import com.flixmate.core.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public Watchlist addToWatchlist(UUID userId, UUID movieId) {
        if (watchlistRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new IllegalStateException("Movie is already in your watchlist.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found."));

        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .movie(movie)
                .build();

        log.info("User {} added movie {} to watchlist.", userId, movieId);
        return watchlistRepository.save(watchlist);
    }

    @Transactional
    public void removeFromWatchlist(UUID userId, UUID movieId) {
        Watchlist watchlist = watchlistRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found in watchlist."));
        watchlistRepository.delete(watchlist);
        log.info("User {} removed movie {} from watchlist.", userId, movieId);
    }

    public List<Watchlist> getWatchlist(UUID userId) {
        return watchlistRepository.findByUserId(userId);
    }
}
