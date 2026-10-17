package com.stockmarket.accountservice.enums;

/**
 * Kinds of charges recorded from statements or manual trades.
 */
public enum ChargeType {

    /**
     * Broker commission.
     */
    BROKERAGE,

    /**
     * Exchange or clearing charges.
     */
    EXCHANGE_FEE,

    /**
     * Securities transaction tax.
     */
    STT,

    /**
     * GST billed as part of charges.
     */
    GST,

    /**
     * Depository participant charges.
     */
    DP_CHARGE,

    /**
     * Blockchain network or withdrawal fee.
     */
    NETWORK_FEE,

    /**
     * Any other charge (stamp duty, SEBI fee, swap, ...).
     */
    OTHER
}
