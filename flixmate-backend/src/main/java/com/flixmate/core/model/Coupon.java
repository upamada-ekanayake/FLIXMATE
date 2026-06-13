package com.flixmate.core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Coupon code is required")
    private String code;

    @Column(nullable = false)
    private String discountType; // "PERCENTAGE", "FLAT_AMOUNT"

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Discount value is required")
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minBookingAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount; // only applicable for PERCENTAGE

    private Integer usageLimit;
    
    @Builder.Default
    private int usageCount = 0;

    @Builder.Default
    private boolean active = true;

    @NotNull(message = "Expiry date is required")
    private LocalDateTime expiryDate;
}
