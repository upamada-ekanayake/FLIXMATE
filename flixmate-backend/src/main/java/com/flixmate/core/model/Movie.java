package com.flixmate.core.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String genre;

    private int durationMinutes;

    private String posterUrl;

    private String trailerUrl;

    private double averageRating;

    private LocalDate releaseDate;

    @Column(unique = true)
    private String tmdbId;
}
