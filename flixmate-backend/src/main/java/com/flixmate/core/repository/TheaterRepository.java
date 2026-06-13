package com.flixmate.core.repository;

import com.flixmate.core.model.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, UUID> {
    List<Theater> findByCity(String city);
}
