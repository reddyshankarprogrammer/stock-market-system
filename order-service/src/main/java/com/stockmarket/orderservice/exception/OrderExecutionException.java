package com.stockmarket.orderservice.exception;

import lombok.Getter;

/**
 * Thrown when an order was persisted but could not be executed.
 * Carries the order id so the client can look up the FAILED order.
 */
@Getter
public class OrderExecutionException extends RuntimeException {

    private final String orderId;

    public OrderExecutionException(String orderId, String message, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
    }
}
