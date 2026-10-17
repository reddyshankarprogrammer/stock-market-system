package com.stockmarket.accountservice.enums;

/**
 * Shape of a trade record.
 */
public enum TradeKind {

    /**
     * A single BUY or SELL fill; changes holdings.
     */
    EXECUTION,

    /**
     * An opened-and-closed position with buy and sell price and realized P&L; does not change holdings.
     */
    CLOSED_POSITION
}
