package com.flixmate.core.service;

import com.flixmate.core.model.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class IntegrationsTest {

    @Autowired
    private AIService aiService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private PaymentService paymentService;

    @Test
    void testGeminiIntegrationFallbackOrResponse() {
        // Test with mock preferences
        String response = aiService.recommendMovies("scifi");
        assertNotNull(response);
        assertTrue(response.length() > 0);
    }

    @Test
    void testTmdbSyncFallbackOrResponse() {
        // Test synchronizing a movie
        Movie movie = movieService.syncMovieFromTmdb("550"); // Fight Club
        assertNotNull(movie);
        assertEquals("550", movie.getTmdbId());
    }

    @Test
    void testStripePaymentIntentCreation() {
        // Test payment intent creation
        String intentId = paymentService.createPaymentIntent(new BigDecimal("15.00"), "usd");
        assertNotNull(intentId);
        assertTrue(intentId.startsWith("pi_"));
        
        // Test payment intent confirmation status check
        boolean success = paymentService.confirmPayment(intentId);
        assertTrue(success);
    }
}
