package com.stockmarket.accountservice.enums;

/**
 * Where an investment account is held.
 *
 * PLATFORM and PAPER accounts trade inside this system at market-data prices and move cash.
 * The other brokers are tracking accounts: their trades are recorded manually or imported
 * from statements and never move cash.
 */
public enum BrokerName {

    /**
     * Traded on this platform (default; all accounts created before v2).
     */
    PLATFORM,

    /**
     * Paper trading on this platform with virtual cash.
     */
    PAPER,

    /**
     * Groww (statement import supported).
     */
    GROWW,

    /**
     * Zerodha (statement import supported).
     */
    ZERODHA,

    /**
     * Binance (statement import supported).
     */
    BINANCE,

    /**
     * Exness (statement import supported).
     */
    EXNESS,

    /**
     * Any other broker; trades are entered manually.
     */
    MANUAL;

    /**
     * True when trades execute here at market price and settle in cash.
     */
    public boolean isPlatformTraded() {
        return this == PLATFORM || this == PAPER;
    }

    /**
     * True when broker statements can be uploaded and parsed for this broker.
     */
    public boolean supportsStatementImport() {
        return this == GROWW || this == ZERODHA || this == BINANCE || this == EXNESS;
    }
}
