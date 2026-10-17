package com.stockmarket.accountservice.enums;

/**
 * Actions recorded in the audit log.
 */
public enum AuditAction {

    /**
     * Account opened.
     */
    CREATE,

    /**
     * Account details changed.
     */
    UPDATE,

    /**
     * Account closed.
     */
    CLOSE,

    /**
     * Cash deposited.
     */
    DEPOSIT,

    /**
     * Cash withdrawn.
     */
    WITHDRAW,

    /**
     * BUY trade executed.
     */
    BUY,

    /**
     * SELL trade executed.
     */
    SELL,

    /**
     * Dividend posted.
     */
    DIVIDEND,

    /**
     * Interest posted.
     */
    INTEREST,

    /**
     * Broker statement uploaded.
     */
    STATEMENT_UPLOAD,

    /**
     * Broker statement parsed and imported.
     */
    STATEMENT_PROCESS,

    /**
     * Broker statement and its imported records deleted.
     */
    STATEMENT_DELETE,

    /**
     * Manual trade recorded.
     */
    TRADE_CREATE,

    /**
     * Holdings changed by a trade or import.
     */
    HOLDING_UPDATE,

    /**
     * Fees calculated for a trade.
     */
    FEE_CALCULATION,

    /**
     * Taxes calculated for a trade.
     */
    TAX_CALCULATION,

    /**
     * Cash adjustment posted.
     */
    ADJUSTMENT
}
