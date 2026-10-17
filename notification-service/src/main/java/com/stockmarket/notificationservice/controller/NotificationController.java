package com.stockmarket.notificationservice.controller;

import com.stockmarket.notificationservice.model.Notification;
import com.stockmarket.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read access to a user's notifications.
 *
 * Base URL:
 * /api/v1/notifications
 *
 * The X-User-Id header is expected to be set by the API gateway
 * after JWT validation.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    @Autowired
    private  NotificationService notificationService;

    /**
     * Returns the authenticated user's notifications, newest first.
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }
}
