package com.flixmate.core.controller;

import com.flixmate.core.dto.ChatRequest;
import com.flixmate.core.dto.ChatResponse;
import com.flixmate.core.model.User;
import com.flixmate.core.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/chatbot/query")
    public ResponseEntity<ChatResponse> queryChatbot(@RequestBody ChatRequest request) {
        User user = null;
        try {
            user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            // Optional: allow unauthenticated chat if needed, but endpoint is secured.
        }
        String response = aiService.chatWithBot(user != null ? user.getId() : null, request.getMessage());
        return ResponseEntity.ok(new ChatResponse(response));
    }

    @GetMapping("/movies/recommend")
    public ResponseEntity<String> recommendMovies(@RequestParam(defaultValue = "popular") String preferences) {
        return ResponseEntity.ok(aiService.recommendMovies(preferences));
    }
}
