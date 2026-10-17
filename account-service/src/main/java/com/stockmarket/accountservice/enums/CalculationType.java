package com.stockmarket.accountservice.enums;

/**
 * How a fee or tax amount is derived from its configured value.
 */
public enum CalculationType {

    /**
     * The configured value is the amount.
     */
    FIXED,

    /**
     * The configured value is a percentage of the base amount.
     */
    PERCENTAGE
}
