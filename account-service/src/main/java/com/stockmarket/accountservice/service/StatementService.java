package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.response.StatementResponse;

import java.time.LocalDate;

/**
 * Account statements.
 */
public interface StatementService {

    /**
     * Statement for the inclusive day range [from, to]: opening and closing balance,
     * ledger with running balance, trades, deposits/withdrawals, dividends, interest,
     * and fee and tax totals.
     */
    StatementResponse generateStatement(String accountId, String userId, LocalDate from, LocalDate to);
}
