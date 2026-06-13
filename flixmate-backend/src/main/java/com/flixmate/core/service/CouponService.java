package com.flixmate.core.service;

import com.flixmate.core.model.Coupon;
import com.flixmate.core.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public Coupon validateCoupon(String code, BigDecimal bookingAmount) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Coupon code not found."));

        if (!coupon.isActive()) {
            throw new IllegalStateException("Coupon is inactive.");
        }

        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Coupon has expired.");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new IllegalStateException("Coupon usage limit has been reached.");
        }

        if (bookingAmount.compareTo(coupon.getMinBookingAmount()) < 0) {
            throw new IllegalStateException("Minimum booking amount of $" + coupon.getMinBookingAmount() + " required to use this coupon.");
        }

        return coupon;
    }

    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        if (couponRepository.findByCodeIgnoreCase(coupon.getCode()).isPresent()) {
            throw new IllegalArgumentException("Coupon code already exists.");
        }
        return couponRepository.save(coupon);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Transactional
    public void incrementUsage(UUID couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found."));
        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);
    }
}
