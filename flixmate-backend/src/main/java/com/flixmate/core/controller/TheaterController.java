package com.flixmate.core.controller;

import com.flixmate.core.model.Theater;
import com.flixmate.core.repository.TheaterRepository;
import com.flixmate.core.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/theaters")
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterRepository theaterRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<Theater>> getAllTheaters() {
        return ResponseEntity.ok(theaterRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'THEATER_MANAGER')")
    public ResponseEntity<Theater> createTheater(@RequestBody Theater theater) {
        Theater saved = theaterRepository.save(theater);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log("CREATE_THEATER", actor, "Created theater: " + saved.getName(), "127.0.0.1");
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'THEATER_MANAGER')")
    public ResponseEntity<Theater> updateTheater(@PathVariable UUID id, @RequestBody Theater details) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Theater not found."));
        theater.setName(details.getName());
        theater.setCity(details.getCity());
        theater.setAddress(details.getAddress());
        theater.setTotalScreens(details.getTotalScreens());
        
        Theater updated = theaterRepository.save(theater);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log("UPDATE_THEATER", actor, "Updated theater: " + updated.getId(), "127.0.0.1");
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTheater(@PathVariable UUID id) {
        theaterRepository.deleteById(id);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log("DELETE_THEATER", actor, "Deleted theater: " + id, "127.0.0.1");
        return ResponseEntity.ok().build();
    }
}
