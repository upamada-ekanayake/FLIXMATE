package com.flixmate.core.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "screens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @Column(nullable = false)
    private String name;

    private int totalSeats;

    @Column(nullable = false)
    @Builder.Default
    private int rowsCount = 8;

    @Column(nullable = false)
    @Builder.Default
    private int colsCount = 10;
}
