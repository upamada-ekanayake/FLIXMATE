package com.flixmate.core.repository;

import com.flixmate.core.model.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {
    List<Screen> findByTheaterId(UUID theaterId);
}
