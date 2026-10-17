package com.stockmarket.accountservice.enums;

/**
 * Direction of a cash movement between the user and the account.
 */
public enum FundTransactionType {

    /**
     * Cash paid into the account.
     */
    DEPOSIT,

    /**
     * Cash paid out of the account.
     */
    WITHDRAWAL,

    /**
     * Manual adjustment that increases cash, for example to match a broker statement.
     */
    ADJUSTMENT_IN,

    /**
     * Manual adjustment that decreases cash.
     */
    ADJUSTMENT_OUT
}
