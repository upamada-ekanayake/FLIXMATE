package com.flixmate.core.controller;

import com.flixmate.core.model.Screen;
import com.flixmate.core.model.Seat;
import com.flixmate.core.model.SeatType;
import com.flixmate.core.repository.ScreenRepository;
import com.flixmate.core.repository.SeatRepository;
import com.flixmate.core.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/screens")
@RequiredArgsConstructor
public class SeatDesignerController {

    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final AuditLogService auditLogService;

    @PostMapping("/{screenId}/design-seats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'THEATER_MANAGER')")
    @Transactional
    public ResponseEntity<List<Seat>> designSeats(
            @PathVariable UUID screenId,
            @RequestParam int rowsCount,
            @RequestParam int colsCount) {

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("Screen not found."));

        screen.setRowsCount(rowsCount);
        screen.setColsCount(colsCount);
        screen.setTotalSeats(rowsCount * colsCount);
        screenRepository.save(screen);

        // Remove existing seats for this screen to design fresh layout
        List<Seat> oldSeats = seatRepository.findByScreenId(screenId);
        seatRepository.deleteAll(oldSeats);

        // Auto-generate fresh seat layout
        // Rows A-B: STANDARD, Rows C-F: PREMIUM, Rows G-H (or last rows): VIP
        List<Seat> newSeats = new ArrayList<>();
        for (int r = 0; r < rowsCount; r++) {
            char rowChar = (char) ('A' + r);
            String rowName = String.valueOf(rowChar);

            // Assign seat type based on row positioning
            SeatType type = SeatType.STANDARD;
            if (r >= 2 && r < 5) {
                type = SeatType.PREMIUM;
            } else if (r >= 5) {
                type = SeatType.VIP;
            }

            for (int c = 1; c <= colsCount; c++) {
                Seat seat = Seat.builder()
                        .screen(screen)
                        .rowName(rowName)
                        .seatNumber(c)
                        .type(type)
                        .build();
                newSeats.add(seat);
            }
        }

        List<Seat> savedSeats = seatRepository.saveAll(newSeats);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log("DESIGN_SEATS", actor, 
                "Designed seat layout for screen " + screenId + " (" + rowsCount + "x" + colsCount + " grid)", 
                "127.0.0.1");

        return ResponseEntity.ok(savedSeats);
    }
}
