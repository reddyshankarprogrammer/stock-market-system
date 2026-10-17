package com.stockmarket.notificationservice.listener;

import com.stockmarket.notificationservice.model.Notification;
import com.stockmarket.notificationservice.model.NotificationType;
import com.stockmarket.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationEventListenerTest {

    private NotificationService notificationService;
    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(2);
        listener = new NotificationEventListener();
        ReflectionTestUtils.setField(listener, "notificationService", notificationService);
    }

    @Test
    void onUserRegistered_sendsWelcome() {
        listener.onUserRegistered(Map.of(
                "userId", "user-1",
                "firstName", "Asha",
                "walletBalance", 10000));

        Notification notification = notificationService.getNotifications("user-1").getFirst();
        assertThat(notification.type()).isEqualTo(NotificationType.WELCOME);
        assertThat(notification.message()).isEqualTo("Welcome Asha! AED 10000.00 has been added to your wallet.");
    }

    @Test
    void onOrderExecuted_usesOrderTypeFromTypeField() {
        listener.onOrderExecuted(orderEvent());

        Notification notification = notificationService.getNotifications("user-1").getFirst();
        assertThat(notification.type()).isEqualTo(NotificationType.ORDER_EXECUTED);
        assertThat(notification.title()).isEqualTo("ORDER EXECUTED - BUY");
        assertThat(notification.message())
                .isEqualTo("Your BUY order for 10 shares of AAPL has been executed at AED 200.00 (total AED 2000.00).");
    }

    @Test
    void onOrderFailed_includesReason() {
        Map<String, Object> event = orderEvent();
        event.put("reason", "Insufficient funds");

        listener.onOrderFailed(event);

        Notification notification = notificationService.getNotifications("user-1").getFirst();
        assertThat(notification.type()).isEqualTo(NotificationType.ORDER_FAILED);
        assertThat(notification.message()).endsWith("Reason: Insufficient funds");
    }

    @Test
    void missingField_throwsIllegalArgument() {
        Map<String, Object> event = orderEvent();
        event.remove("userId");

        assertThatThrownBy(() -> listener.onOrderExecuted(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void notificationsAreNewestFirstAndBoundedPerUser() {
        notificationService.send("user-1", NotificationType.WELCOME, "t", "first");
        notificationService.send("user-1", NotificationType.WELCOME, "t", "second");
        notificationService.send("user-1", NotificationType.WELCOME, "t", "third");

        List<Notification> notifications = notificationService.getNotifications("user-1");
        assertThat(notifications).extracting(Notification::message).containsExactly("third", "second");
        assertThat(notificationService.getNotifications("unknown")).isEmpty();
    }

    private Map<String, Object> orderEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", "order-1");
        event.put("userId", "user-1");
        event.put("symbol", "AAPL");
        event.put("type", "BUY");
        event.put("quantity", 10);
        event.put("price", 200.0);
        event.put("totalAmount", 2000.0);
        event.put("status", "EXECUTED");
        return event;
    }
}
