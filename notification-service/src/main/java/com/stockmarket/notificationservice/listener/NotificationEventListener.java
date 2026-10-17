package com.stockmarket.notificationservice.listener;

import com.stockmarket.notificationservice.model.NotificationType;
import com.stockmarket.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Consumes user and order events from Kafka and turns them into notifications.
 *
 * Consumer group comes from spring.kafka.consumer.group-id. Events with missing
 * fields throw IllegalArgumentException, which the error handler skips without retrying.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    @Autowired
    private  NotificationService notificationService;

    /**
     * user.registered, published by user-service:
     * {userId, email, firstName, lastName, walletBalance}
     */
    @KafkaListener(topics = "${kafka.topics.user-registered}")
    public void onUserRegistered(@Payload Map<String, Object> payload) {

        log.debug("Received user.registered event: {}", payload);

        String userId = required(payload, "userId");
        String firstName = required(payload, "firstName");
        BigDecimal walletBalance = amount(payload, "walletBalance");

        notificationService.send(
                userId,
                NotificationType.WELCOME,
                "WELCOME TO STOCK TRADING PLATFORM",
                String.format("Welcome %s! AED %s has been added to your wallet.",
                        firstName, walletBalance));
    }

    /**
     * order.executed, published by order-service:
     * {orderId, userId, symbol, type, quantity, price, totalAmount, status, timestamp}
     */
    @KafkaListener(topics = "${kafka.topics.order-executed}")
    public void onOrderExecuted(@Payload Map<String, Object> payload) {

        log.debug("Received order.executed event: {}", payload);

        String userId = required(payload, "userId");
        String orderType = required(payload, "type");
        String symbol = required(payload, "symbol");
        int quantity = Integer.parseInt(required(payload, "quantity"));
        BigDecimal price = amount(payload, "price");
        BigDecimal totalAmount = amount(payload, "totalAmount");

        notificationService.send(
                userId,
                NotificationType.ORDER_EXECUTED,
                "ORDER EXECUTED - " + orderType,
                String.format("Your %s order for %d shares of %s has been executed at AED %s (total AED %s).",
                        orderType, quantity, symbol, price, totalAmount));
    }

    /**
     * order.failed, published by order-service:
     * same fields as order.executed plus an optional reason.
     */
    @KafkaListener(topics = "${kafka.topics.order-failed}")
    public void onOrderFailed(@Payload Map<String, Object> payload) {

        log.debug("Received order.failed event: {}", payload);

        String userId = required(payload, "userId");
        String orderType = required(payload, "type");
        String symbol = required(payload, "symbol");
        int quantity = Integer.parseInt(required(payload, "quantity"));
        Object reason = payload.get("reason");

        notificationService.send(
                userId,
                NotificationType.ORDER_FAILED,
                "ORDER FAILED - " + orderType,
                String.format("Your %s order for %d shares of %s could not be executed.%s",
                        orderType, quantity, symbol,
                        reason == null ? "" : " Reason: " + reason));
    }

    private String required(Map<String, Object> payload, String field) {

        Object value = payload.get(field);

        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Event is missing field '" + field + "': " + payload);
        }

        return value.toString();
    }

    private BigDecimal amount(Map<String, Object> payload, String field) {

        return new BigDecimal(required(payload, field)).setScale(2, RoundingMode.HALF_UP);
    }
}
