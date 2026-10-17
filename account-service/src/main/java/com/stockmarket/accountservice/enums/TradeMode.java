package com.stockmarket.accountservice.enums;

/**
 * How a broker's trade rows are laid out.
 */
public enum TradeMode {

    /**
     * One row per fill (BUY or SELL).
     */
    EXECUTION,

    /**
     * One row per closed position with open and close price and P&L.
     */
    CLOSED_POSITION
}
