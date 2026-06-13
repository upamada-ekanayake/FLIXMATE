package com.flixmate.core.service;

import com.flixmate.core.model.ChatHistory;
import com.flixmate.core.model.SentimentType;
import com.flixmate.core.model.Showtime;
import com.flixmate.core.repository.ChatHistoryRepository;
import com.flixmate.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final Bucket geminiRateLimiter = Bucket.builder()
            .addLimit(Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1))))
            .build();

    @Value("${flixmate.gemini.api-key}")
    private String geminiApiKey;

    @Value("${flixmate.gemini.base-url}")
    private String geminiUrl;

    /**
     * Helper to call Gemini 2.5 Flash API
     */
    private String callGemini(String prompt) {
        if ("mock-key".equals(geminiApiKey) || geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Falling back to local computation.");
            return null;
        }

        int maxRetries = 3;
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (!geminiRateLimiter.tryConsume(1)) {
                log.warn("Gemini client-side rate limit hit. Waiting 1 second before retry (attempt {})...", attempt);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                continue;
            }

            try {
                log.info("Calling Gemini API. Attempt {}/{}", attempt, maxRetries);
                String url = geminiUrl;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-goog-api-key", geminiApiKey);

                Map<String, Object> textObj = Map.of("text", prompt);
                Map<String, Object> partsObj = Map.of("parts", List.of(textObj));
                Map<String, Object> contentsObj = Map.of("contents", List.of(partsObj));

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(contentsObj, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List candidates = (List) response.getBody().get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map candidate = (Map) candidates.get(0);
                        Map content = (Map) candidate.get("content");
                        if (content != null) {
                            List parts = (List) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                Map part = (Map) parts.get(0);
                                return (String) part.get("text");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Gemini API call failed on attempt {}/{} with error: {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    break;
                }
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                backoffMs *= 2; // exponential backoff
            }
        }
        return null;
    }

    /**
     * 1. AI Movie Recommendation Engine
     */
    public String recommendMovies(String userPreferences) {
        String prompt = "You are a movie recommendation expert on FlixMate. " +
                "Recommend 5 movies based on these preferences: " + userPreferences + ". " +
                "Format the response in short bullet points detailing movie titles and brief reasons why they match.";
        
        String response = callGemini(prompt);
        if (response != null) {
            return response;
        }

        // Local Rule-Based Fallback
        log.info("Running local movie recommendation fallback.");
        String prefs = userPreferences.toLowerCase();
        if (prefs.contains("action") || prefs.contains("scifi") || prefs.contains("adventure")) {
            return "* **Interstellar**: Epic sci-fi adventure exploring space and time.\n" +
                   "* **Inception**: Intense mind-bending thriller in sleep realms.\n" +
                   "* **Gladiator**: Gritty action-adventure of honor and survival.";
        } else if (prefs.contains("comedy") || prefs.contains("funny")) {
            return "* **The Hangover**: Hilarious bachelor party comedy.\n" +
                   "* **Superbad**: Classic teenage comedy adventure.\n" +
                   "* **Knives Out**: Modern comedy-mystery with humor and twist plots.";
        } else {
            return "* **The Shawshank Redemption**: Inspirational story of friendship and hope.\n" +
                   "* **The Godfather**: Masterpiece of crime and family loyalty.\n" +
                   "* **The Dark Knight**: Modern thriller that redefines comic-book cinema.";
        }
    }

    /**
     * 2. AI Customer Support Chatbot
     */
    public String chatWithBot(UUID userId, String userMessage) {
        String prompt = "You are FlixMate's virtual assistant, a premium cinema ticket support AI. " +
                "Answer the customer's query: '" + userMessage + "'. " +
                "Help with reservations, seat locking (10-minute limits), cancellations, and ticket generation. " +
                "Keep the response helpful, engaging, and professional (under 100 words).";

        String response = callGemini(prompt);
        if (response == null) {
            // Local Rule-Based Chatbot Fallback
            log.info("Running local chatbot reply fallback.");
            String msg = userMessage.toLowerCase();
            if (msg.contains("refund") || msg.contains("cancel")) {
                response = "You can cancel a booking up to 2 hours before showtime. Refunds will be processed back to your original payment method in 3-5 business days.";
            } else if (msg.contains("seat") || msg.contains("hold") || msg.contains("lock")) {
                response = "Seats are locked in for 10 minutes during checkout to allow you to finish payment securely. Once paid, your booking is confirmed.";
            } else if (msg.contains("ticket") || msg.contains("qr") || msg.contains("pdf")) {
                response = "You can download your tickets in PDF format with QR codes right from your dashboard after confirming payment.";
            } else {
                response = "Welcome to FlixMate! I'm here to assist you with movie recommendations, ticket booking details, seat locks, or theater showtimes. How can I help you today?";
            }
        }

        // Save history in background
        try {
            ChatHistory history = ChatHistory.builder()
                    .user(userId != null ? userRepository.findById(userId).orElse(null) : null)
                    .message(userMessage)
                    .response(response)
                    .build();
            chatHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("Could not write chat history to database.", e);
        }

        return response;
    }

    /**
     * 3. AI Sentiment Analysis
     */
    public SentimentType analyzeSentiment(String reviewText) {
        String prompt = "Classify the sentiment of the following movie review text into exactly one word: 'POSITIVE', 'NEUTRAL', or 'NEGATIVE'. " +
                "Review: \"" + reviewText + "\"";

        String response = callGemini(prompt);
        if (response != null) {
            String clean = response.trim().toUpperCase();
            if (clean.contains("POSITIVE")) return SentimentType.POSITIVE;
            if (clean.contains("NEGATIVE")) return SentimentType.NEGATIVE;
            if (clean.contains("NEUTRAL")) return SentimentType.NEUTRAL;
        }

        // Local Dictionary-based sentiment fallback
        log.info("Running local sentiment evaluation fallback.");
        String text = reviewText.toLowerCase();
        int score = 0;
        String[] positiveWords = {"good", "great", "excellent", "amazing", "love", "awesome", "masterpiece", "beautiful", "must watch"};
        String[] negativeWords = {"bad", "worst", "boring", "waste", "hate", "terrible", "awful", "disappointed", "poor"};

        for (String w : positiveWords) {
            if (text.contains(w)) score++;
        }
        for (String w : negativeWords) {
            if (text.contains(w)) score--;
        }

        if (score > 0) return SentimentType.POSITIVE;
        if (score < 0) return SentimentType.NEGATIVE;
        return SentimentType.NEUTRAL;
    }

    /**
     * 4. AI Dynamic Pricing Engine
     */
    public BigDecimal calculateDynamicPrice(Showtime showtime, double occupancyRate) {
        BigDecimal base = showtime.getBasePrice();
        BigDecimal multiplier = BigDecimal.ONE;

        // Occupancy factor: high demand raises price
        if (occupancyRate > 0.8) {
            multiplier = multiplier.add(new BigDecimal("0.25")); // +25%
        } else if (occupancyRate > 0.5) {
            multiplier = multiplier.add(new BigDecimal("0.10")); // +10%
        }

        // Time factor: evening showtimes (after 5 PM) are premium
        LocalTime startTime = showtime.getStartTime().toLocalTime();
        if (startTime.isAfter(LocalTime.of(17, 0)) && startTime.isBefore(LocalTime.of(22, 30))) {
            multiplier = multiplier.add(new BigDecimal("0.15")); // +15%
        }

        BigDecimal finalPrice = base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        log.info("Dynamic pricing calculated: base={}, occupancy={}, time={}, final={}", 
                base, occupancyRate, startTime, finalPrice);
        return finalPrice;
    }

    /**
     * 5. AI Admin Revenue Prediction (Linear Regression)
     */
    public BigDecimal predictRevenueNextMonth(List<BigDecimal> weeklyHistory) {
        if (weeklyHistory == null || weeklyHistory.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Try Gemini analysis first
        String prompt = "Based on this historical weekly revenue sequence: " + weeklyHistory + ", " +
                "estimate the total revenue for the next 4 weeks. Return ONLY a single decimal number without any text or formatting.";
        String geminiResult = callGemini(prompt);
        if (geminiResult != null) {
            try {
                String clean = geminiResult.replaceAll("[^0-9.]", "").trim();
                return new BigDecimal(clean);
            } catch (Exception e) {
                log.warn("Gemini output was not a valid decimal. Falling back to local regression.");
            }
        }

        // Local regression/smoothing fallback: y = mx + c
        log.info("Running local linear regression for revenue prediction.");
        int n = weeklyHistory.size();
        if (n == 1) {
            return weeklyHistory.get(0).multiply(new BigDecimal("4"));
        }

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = weeklyHistory.get(i).doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // Predict next 4 weeks: sum of x from (n+1) to (n+4)
        double predictedTotal = 0;
        for (int i = 1; i <= 4; i++) {
            double nextX = n + i;
            predictedTotal += (slope * nextX + intercept);
        }

        return BigDecimal.valueOf(Math.max(predictedTotal, 0)).setScale(2, RoundingMode.HALF_UP);
    }
}
