package com.stockmarket.accountservice.enums;

/**
 * Side of a ledger entry.
 */
public enum LedgerEntryType {

    /**
     * Reduces the account balance.
     */
    DEBIT,

    /**
     * Increases the account balance.
     */
    CREDIT
}
