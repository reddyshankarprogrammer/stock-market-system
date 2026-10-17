package com.stockmarket.orderservice.dto;

import com.stockmarket.orderservice.entity.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO used to place a stock order.
 *
 * Example Request:
 *
 * {
 *   "symbol": "AAPL",
 *   "type": "BUY",
 *   "quantity": 10
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    /**
     * Unique identifier of the user placing the order.
     * Always overwritten from the X-User-Id header by the controller.
     */
    private String userId;

    /**
     * Stock symbol.
     * Examples: AAPL, GOOG, MSFT, TSLA
     */
    @NotBlank(message = "Stock symbol is required")
    private String symbol;

    /**
     * Order type.
     * BUY or SELL.
     */
    @NotNull(message = "Order type is required")
    private OrderType type;

    /**
     * Number of shares to buy or sell.
     */
    @Min(value = 1, message = "Quantity must be greater than zero")
    private int quantity;

    /**
     * Requested price per share. Optional and currently ignored:
     * orders are market orders priced from market-data-service.
     */
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;
}