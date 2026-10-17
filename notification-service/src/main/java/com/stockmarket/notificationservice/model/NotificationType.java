package com.stockmarket.notificationservice.model;

/**
 * Kind of event a notification was raised for.
 */
public enum NotificationType {

    /**
     * New user registered (user.registered).
     */
    WELCOME,

    /**
     * Order executed successfully (order.executed).
     */
    ORDER_EXECUTED,

    /**
     * Order execution failed (order.failed).
     */
    ORDER_FAILED
}
