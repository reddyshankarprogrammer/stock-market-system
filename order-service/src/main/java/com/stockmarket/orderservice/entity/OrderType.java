package com.stockmarket.orderservice.entity;

/**
 * Represents the type of stock order.
 *
 * BUY  - Purchase shares of a stock.
 * SELL - Sell owned shares of a stock.
 *
 * Examples:
 *
 * BUY:
 * User purchases 10 shares of AAPL.
 *
 * SELL:
 * User sells 5 shares of TSLA.
 */
public enum OrderType {

    /**
     * Buy stock shares.
     */
    BUY,

    /**
     * Sell stock shares.
     */
    SELL
}