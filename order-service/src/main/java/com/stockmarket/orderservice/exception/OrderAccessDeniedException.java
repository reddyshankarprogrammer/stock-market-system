package com.stockmarket.orderservice.exception;

public class OrderAccessDeniedException extends RuntimeException {

    public OrderAccessDeniedException(String orderId) {
        super("Access denied to order: " + orderId);
    }
}
