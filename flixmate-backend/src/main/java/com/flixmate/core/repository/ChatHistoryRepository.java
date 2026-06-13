package com.flixmate.core.repository;

import com.flixmate.core.model.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, UUID> {
    List<ChatHistory> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
