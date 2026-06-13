package com.flixmate.core.controller;

import com.flixmate.core.model.Coupon;
import com.flixmate.core.service.AuditLogService;
import com.flixmate.core.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MARKETING_AGENT')")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MARKETING_AGENT')")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        Coupon created = couponService.createCoupon(coupon);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log("CREATE_COUPON", actor, "Created promo coupon code: " + created.getCode(), "127.0.0.1");
        return ResponseEntity.ok(created);
    }

    @GetMapping("/validate")
    public ResponseEntity<Coupon> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(couponService.validateCoupon(code, amount));
    }
}
