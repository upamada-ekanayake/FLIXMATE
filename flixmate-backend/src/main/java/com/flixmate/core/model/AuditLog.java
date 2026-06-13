package com.flixmate.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String action; // e.g. "CREATE_THEATER", "DELETE_SHOWTIME", "APPLY_COUPON"

    @Column(nullable = false)
    private String actor; // email or user context identifier

    @Column(length = 2000)
    private String details;

    private String ipAddress;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime timestamp;
}
