package com.stockmarket.notificationservice.service;

import com.stockmarket.notificationservice.model.Notification;
import com.stockmarket.notificationservice.model.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Creates notifications and keeps the most recent ones per user in memory.
 *
 * Delivery is currently log-only.
 * TODO: Push notification / Email / SMS implementation
 */
@Slf4j
@Service
public class NotificationService {

    private final int maxPerUser;

    private final Map<String, Deque<Notification>> notificationsByUser = new ConcurrentHashMap<>();

    public NotificationService(@Value("${notification.max-per-user:100}") int maxPerUser) {
        this.maxPerUser = maxPerUser;
    }

    /**
     * Creates, stores and delivers a notification.
     *
     * @return the created notification
     */
    public Notification send(String userId,
                             NotificationType type,
                             String title,
                             String message) {

        Notification notification = new Notification(
                UUID.randomUUID().toString(),
                userId,
                type,
                title,
                message,
                LocalDateTime.now());

        Deque<Notification> notifications =
                notificationsByUser.computeIfAbsent(userId, id -> new ConcurrentLinkedDeque<>());
        notifications.addFirst(notification);
        while (notifications.size() > maxPerUser) {
            notifications.pollLast();
        }

        log.info("Notification sent. UserId={}, Type={}, Title={}, Message={}",
                userId, type, title, message);

        return notification;
    }

    /**
     * Returns a user's notifications, newest first.
     */
    public List<Notification> getNotifications(String userId) {

        Deque<Notification> notifications = notificationsByUser.get(userId);

        return notifications == null ? List.of() : new ArrayList<>(notifications);
    }
}
