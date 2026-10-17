package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.request.CreateAccountRequest;
import com.stockmarket.accountservice.dto.request.UpdateAccountRequest;
import com.stockmarket.accountservice.dto.response.AccountResponse;
import com.stockmarket.accountservice.dto.response.AccountSummaryResponse;
import com.stockmarket.accountservice.dto.response.AuditLogResponse;

import java.util.List;

/**
 * Investment account lifecycle and overview. All methods act on behalf of
 * {@code userId} (the caller) and enforce ownership.
 */
public interface AccountService {

    /**
     * Opens an account for the caller after checking with user-service that they exist and are ACTIVE.
     */
    AccountResponse createAccount(String userId, CreateAccountRequest request);

    AccountResponse getAccount(String accountId, String userId);

    AccountResponse updateAccount(String accountId, String userId, UpdateAccountRequest request);

    /**
     * Closes the account (soft delete). The balance must be zero and no shares may be held,
     * so nothing is lost; the account and its history stay readable.
     */
    AccountResponse deleteAccount(String accountId, String userId);

    /**
     * Accounts of {@code userId}: allowed when it is the caller, or for an ADMIN caller.
     */
    List<AccountResponse> getUserAccounts(String userId, String callerId);

    AccountSummaryResponse getSummary(String accountId, String userId);

    List<AuditLogResponse> getAuditTrail(String accountId, String userId);
}
