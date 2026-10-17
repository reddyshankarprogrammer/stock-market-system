package com.stockmarket.accountservice.enums;

/**
 * Lifecycle state of an investment account.
 */
public enum AccountStatus {

    /**
     * Open; deposits, withdrawals and trades are allowed.
     */
    ACTIVE,

    /**
     * Temporarily frozen; read-only.
     */
    SUSPENDED,

    /**
     * Closed by the owner; read-only.
     */
    CLOSED
}
