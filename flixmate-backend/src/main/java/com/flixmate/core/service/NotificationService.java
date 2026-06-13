package com.flixmate.core.service;

import com.flixmate.core.model.Notification;
import com.flixmate.core.model.User;
import com.flixmate.core.repository.NotificationRepository;
import com.flixmate.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification sendNotification(UUID userId, String title, String message, String type) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));
        }

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .build();

        log.info("Sending notification: title='{}', type='{}', toUser={}", title, type, userId != null ? userId : "ALL");
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found."));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
