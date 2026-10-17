package com.stockmarket.accountservice.enums;

/**
 * Business reason for a ledger entry.
 */
public enum LedgerCategory {

    /**
     * Cash deposit.
     */
    DEPOSIT,

    /**
     * Cash withdrawal.
     */
    WITHDRAWAL,

    /**
     * Gross value of a BUY trade.
     */
    TRADE_BUY,

    /**
     * Gross proceeds of a SELL trade.
     */
    TRADE_SELL,

    /**
     * Brokerage, platform, exchange or withdrawal fee.
     */
    FEE,

    /**
     * Trade, capital gains or dividend tax.
     */
    TAX,

    /**
     * Gross dividend received.
     */
    DIVIDEND,

    /**
     * Interest paid on the cash balance.
     */
    INTEREST,

    /**
     * Cash adjustment to reconcile with a broker statement.
     */
    STATEMENT_ADJUSTMENT
}
