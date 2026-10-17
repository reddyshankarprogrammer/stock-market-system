package com.stockmarket.accountservice.enums;

/**
 * Lifecycle of an uploaded statement.
 */
public enum StatementStatus {

    /**
     * Stored; not processed yet.
     */
    UPLOADED,

    /**
     * Being parsed and imported.
     */
    PROCESSING,

    /**
     * Imported without warnings.
     */
    PROCESSED,

    /**
     * Imported with skipped rows or warnings.
     */
    PARTIALLY_PROCESSED,

    /**
     * Could not be imported; see the import history.
     */
    FAILED
}
