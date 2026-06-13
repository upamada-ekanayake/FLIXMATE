package com.flixmate.core.repository;

import com.flixmate.core.model.BookedSeat;
import com.flixmate.core.model.BookedSeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookedSeatRepository extends JpaRepository<BookedSeat, UUID> {

    @Query("SELECT bs FROM BookedSeat bs WHERE bs.booking.showtime.id = :showtimeId AND " +
           "(bs.status = 'BOOKED' OR (bs.status = 'HOLD' AND bs.holdExpiresAt > CURRENT_TIMESTAMP))")
    List<BookedSeat> findOccupiedSeatsByShowtime(@Param("showtimeId") UUID showtimeId);

    List<BookedSeat> findByStatusAndHoldExpiresAtBefore(BookedSeatStatus status, LocalDateTime dateTime);
}
