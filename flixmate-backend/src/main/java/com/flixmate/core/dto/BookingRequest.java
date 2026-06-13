package com.flixmate.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    private UUID showtimeId;
    private List<UUID> seatIds;
    private String couponCode;
    private int redeemPoints = 0;
}
