package com.flixmate.core.repository;

import com.flixmate.core.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {
    List<Watchlist> findByUserId(UUID userId);
    Optional<Watchlist> findByUserIdAndMovieId(UUID userId, UUID movieId);
    boolean existsByUserIdAndMovieId(UUID userId, UUID movieId);
}
