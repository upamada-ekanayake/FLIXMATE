package com.flixmate.core.repository;

import com.flixmate.core.model.Booking;
import com.flixmate.core.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime dateTime);
}
