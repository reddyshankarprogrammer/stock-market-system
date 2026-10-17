package com.stockmarket.accountservice.enums;

/**
 * How a trade, charge or tax record entered the system.
 */
public enum RecordSource {

    /**
     * Created by this platform's trade execution or fee/tax engines.
     */
    PLATFORM,

    /**
     * Entered by the user.
     */
    MANUAL,

    /**
     * Imported from a broker statement.
     */
    IMPORT
}
