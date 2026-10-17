package com.stockmarket.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a buy or sell order placed by a user.
 *
 * Lifecycle:
 * CREATED -> PENDING -> EXECUTED
 *                    -> FAILED
 *                    -> CANCELLED
 *
 * Example:
 * User places BUY order for 10 AAPL shares at $200.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "userId"),
                @Index(name = "idx_orders_symbol", columnList = "symbol"),
                @Index(name = "idx_orders_status", columnList = "status")
        }
)
public class Order {

    /**
     * Unique order identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * User who placed the order.
     */
    @Column(nullable = false)
    private String userId;

    /**
     * Stock symbol.
     * Examples: AAPL, GOOG, MSFT, TSLA
     */
    @Column(nullable = false)
    private String symbol;

    /**
     * BUY or SELL.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    /**
     * Current order status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /**
     * Number of shares requested.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Execution price per share.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /**
     * quantity × price
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Failure reason when an order cannot be executed.
     *
     * Examples:
     * - Insufficient Funds
     * - Stock Not Found
     * - Market Closed
     */
    private String failureReason;

    /**
     * Timestamp when the order was created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the order was successfully executed.
     * Null until execution.
     */
    private LocalDateTime executedAt;

    /**
     * Automatically populate creation timestamp.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.totalAmount == null
                && this.price != null) {
            this.totalAmount =
                    this.price.multiply(
                            BigDecimal.valueOf(this.quantity));
        }
    }
}