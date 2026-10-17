package com.stockmarket.accountservice.statement.service;

import com.stockmarket.accountservice.entity.StatementImportHistory;

/**
 * Parses a stored statement and imports its trades, charges and taxes.
 *
 * Every call is one import attempt with its own {@link StatementImportHistory} row. Before
 * importing, everything the statement imported earlier is removed, so reprocessing (for
 * example after fixing a column mapping) is safe and never duplicates data.
 */
public interface StatementImportService {

    /**
     * Never throws for a bad file: failures are recorded as a FAILED attempt and returned.
     */
    StatementImportHistory process(String statementId);

    /**
     * Removes the trades, charges and taxes imported from a statement and rebuilds holdings.
     * Must run inside the caller's transaction.
     */
    void removeImportedData(String statementId);
}
