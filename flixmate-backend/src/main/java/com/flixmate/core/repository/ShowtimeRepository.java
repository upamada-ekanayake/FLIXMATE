package com.flixmate.core.repository;

import com.flixmate.core.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {
    List<Showtime> findByMovieId(UUID movieId);

    @Query("SELECT s FROM Showtime s WHERE s.movie.id = :movieId AND s.screen.theater.id = :theaterId")
    List<Showtime> findByMovieAndTheater(@Param("movieId") UUID movieId, @Param("theaterId") UUID theaterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Showtime s WHERE s.id = :id")
    Optional<Showtime> findByIdWithWriteLock(@Param("id") UUID id);
}
