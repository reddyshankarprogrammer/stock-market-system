package com.stockmarket.accountservice.enums;

/**
 * Kinds of fees the fee engine can charge.
 */
public enum FeeType {

    /**
     * Broker commission on a trade.
     */
    BROKERAGE,

    /**
     * Platform charge per trade.
     */
    PLATFORM,

    /**
     * Exchange/clearing levy on a trade.
     */
    EXCHANGE,

    /**
     * Charge for withdrawing cash.
     */
    WITHDRAWAL
}
