package com.flixmate.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private UUID bookingId;
    private UUID showtimeId;
    private String movieTitle;
    private String theaterName;
    private String screenName;
    private String startTime;
    private BigDecimal totalPrice;
    private String status;
    private List<String> seatLabels; // e.g. ["A-5", "A-6"]
    private String qrCodeBase64;
    private String paymentIntentId;
}
