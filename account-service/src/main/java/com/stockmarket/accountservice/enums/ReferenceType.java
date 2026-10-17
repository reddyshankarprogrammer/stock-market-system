package com.stockmarket.accountservice.enums;

/**
 * Kind of business record a ledger entry points to.
 */
public enum ReferenceType {

    /**
     * An AccountFundTransaction.
     */
    FUND_TRANSACTION,

    /**
     * A Trade.
     */
    TRADE,

    /**
     * A Dividend.
     */
    DIVIDEND,

    /**
     * An InterestTransaction.
     */
    INTEREST
}
