package com.stockmarket.accountservice.enums;

/**
 * Outcome of a fund transaction.
 */
public enum TransactionStatus {

    /**
     * Posted to the ledger.
     */
    COMPLETED,

    /**
     * Rejected; nothing was posted.
     */
    FAILED
}
