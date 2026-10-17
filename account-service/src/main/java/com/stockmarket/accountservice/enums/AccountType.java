package com.stockmarket.accountservice.enums;

/**
 * Type of investment account.
 */
public enum AccountType {

    /**
     * Owned by one person.
     */
    INDIVIDUAL,

    /**
     * Owned jointly by more than one person.
     */
    JOINT,

    /**
     * Owned by a company.
     */
    CORPORATE,

    /**
     * Cash equities (for example Groww Main, Zerodha Equity).
     */
    EQUITY,

    /**
     * Futures and options (for example Zerodha F&O).
     */
    DERIVATIVES,

    /**
     * Crypto assets (for example Binance).
     */
    CRYPTO,

    /**
     * Currency and CFD trading (for example Exness).
     */
    FOREX
}
