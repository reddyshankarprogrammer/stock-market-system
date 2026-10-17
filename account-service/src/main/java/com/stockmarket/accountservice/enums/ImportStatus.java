package com.stockmarket.accountservice.enums;

/**
 * Outcome of one import attempt.
 */
public enum ImportStatus {

    /**
     * All rows imported.
     */
    SUCCESS,

    /**
     * Imported, but some rows were skipped or produced warnings.
     */
    PARTIAL,

    /**
     * Nothing was imported.
     */
    FAILED
}
