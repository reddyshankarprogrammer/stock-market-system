package com.stockmarket.orderservice.entity;

/**
 * Represents the current state of an order.
 *
 * Order Lifecycle:
 *
 * CREATED
 *    |
 *    v
 * PENDING
 *    |
 *    +------> EXECUTED
 *    |
 *    +------> FAILED
 *    |
 *    +------> CANCELLED
 *
 * CREATED   : Order has been accepted by the system.
 * PENDING   : Order is awaiting processing or execution.
 * EXECUTED  : Order has been successfully executed.
 * FAILED    : Order execution failed.
 * CANCELLED : Order was cancelled before execution.
 */
public enum OrderStatus {

    /**
     * Order has been created and validated.
     */
    CREATED,

    /**
     * Order is waiting for execution.
     */
    PENDING,

    /**
     * Order has been successfully executed.
     */
    EXECUTED,

    /**
     * Order execution failed.
     */
    FAILED,

    /**
     * Order was cancelled by the user or system.
     */
    CANCELLED
}