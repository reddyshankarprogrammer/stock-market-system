package com.stockmarket.accountservice.enums;

/**
 * Kinds of taxes the tax engine can charge.
 */
public enum TaxType {

    /**
     * Transaction tax on the trade value.
     */
    TRADE_TAX,

    /**
     * Tax on positive realized profit of a SELL.
     */
    CAPITAL_GAINS_TAX,

    /**
     * Withholding tax on a gross dividend.
     */
    DIVIDEND_TAX,

    /**
     * Goods and services tax (for example GST on brokerage).
     */
    GST,

    /**
     * Value-added tax.
     */
    VAT,

    /**
     * Tax deducted at source.
     */
    TDS
}
