package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.request.AdjustmentRequest;
import com.stockmarket.accountservice.dto.request.DepositRequest;
import com.stockmarket.accountservice.dto.request.WithdrawRequest;
import com.stockmarket.accountservice.dto.response.FundTransactionResponse;

import java.util.List;

/**
 * Cash deposits and withdrawals.
 */
public interface FundService {

    /**
     * Increases the balance, records a fund transaction and a CREDIT ledger entry.
     */
    FundTransactionResponse deposit(String accountId, String userId, DepositRequest request);

    /**
     * Checks the balance covers amount + withdrawal fee, then records a fund transaction,
     * a WITHDRAWAL debit and a FEE debit.
     */
    FundTransactionResponse withdraw(String accountId, String userId, WithdrawRequest request);

    /**
     * Posts a STATEMENT_ADJUSTMENT credit (positive amount) or debit (negative amount),
     * typically to reconcile a tracking account's cash with a broker statement.
     */
    FundTransactionResponse adjust(String accountId, String userId, AdjustmentRequest request);

    List<FundTransactionResponse> getFundTransactions(String accountId, String userId);
}
