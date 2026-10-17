package com.stockmarket.notificationservice.model;

import java.time.LocalDateTime;

/**
 * A notification delivered to a user.
 */
public record Notification(
        String id,
        String userId,
        NotificationType type,
        String title,
        String message,
        LocalDateTime createdAt) {
}
