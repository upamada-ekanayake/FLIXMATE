package com.flixmate.core.controller;

import com.flixmate.core.model.Movie;
import com.flixmate.core.model.Review;
import com.flixmate.core.model.SentimentType;
import com.flixmate.core.model.User;
import com.flixmate.core.repository.MovieRepository;
import com.flixmate.core.repository.ReviewRepository;
import com.flixmate.core.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final AIService aiService;

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<Review>> getReviewsByMovie(@PathVariable UUID movieId) {
        return ResponseEntity.ok(reviewRepository.findByMovieIdOrderByCreatedAtDesc(movieId));
    }

    @PostMapping("/movie/{movieId}")
    public ResponseEntity<Review> submitReview(
            @PathVariable UUID movieId,
            @RequestParam int rating,
            @RequestParam String comment
    ) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found."));

        // Analyze sentiment via AI Engine
        SentimentType sentiment = aiService.analyzeSentiment(comment);

        Review review = Review.builder()
                .user(user)
                .movie(movie)
                .rating(rating)
                .comment(comment)
                .sentiment(sentiment)
                .build();

        Review savedReview = reviewRepository.save(review);
        
        // Update movie average rating
        List<Review> movieReviews = reviewRepository.findByMovieIdOrderByCreatedAtDesc(movieId);
        double avg = movieReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        movie.setAverageRating(Math.round(avg * 10.0) / 10.0);
        movieRepository.save(movie);

        return ResponseEntity.ok(savedReview);
    }
}
