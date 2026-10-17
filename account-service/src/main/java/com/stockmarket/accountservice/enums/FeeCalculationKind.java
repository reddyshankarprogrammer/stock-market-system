package com.stockmarket.accountservice.enums;

/**
 * What a fee preview is calculated for.
 */
public enum FeeCalculationKind {

    /**
     * A BUY or SELL trade: brokerage, platform and exchange fees.
     */
    TRADE,

    /**
     * A cash withdrawal: withdrawal fee.
     */
    WITHDRAWAL
}
