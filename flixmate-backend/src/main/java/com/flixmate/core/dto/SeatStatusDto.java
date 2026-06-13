package com.flixmate.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatStatusDto {
    private UUID seatId;
    private String rowName;
    private int seatNumber;
    private String type; // STANDARD, PREMIUM, VIP
    private String status; // AVAILABLE, HOLD, BOOKED
}
