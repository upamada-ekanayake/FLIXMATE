package com.flixmate.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAnalyticsDto {
    private BigDecimal totalRevenue;
    private BigDecimal predictedRevenueNextMonth; // AI Generated
    private double averageOccupancyRate;
    private long totalTicketsSold;
    private Map<String, Long> reviewSentimentCounts; // POSITIVE, NEUTRAL, NEGATIVE
    private Map<String, BigDecimal> weeklyRevenueHistory; // Date -> Revenue
}
