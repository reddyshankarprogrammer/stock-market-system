package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.exception.AccountAccessDeniedException;
import com.stockmarket.accountservice.exception.ResourceNotFoundException;
import com.stockmarket.accountservice.exception.ValidationException;

/**
 * Loads accounts with the ownership and status checks every operation needs.
 *
 * Kept separate from {@link AccountService} so the other services can use it
 * without depending on each other.
 */
public interface AccountAccessService {

    /**
     * Account owned by the caller (or any account for an ADMIN caller), without locking (for reads).
     *
     * @throws ResourceNotFoundException    if the account does not exist
     * @throws AccountAccessDeniedException if it belongs to another user
     */
    InvestmentAccount getOwnedAccount(String accountId, String userId);

    /**
     * Account owned by the caller, row-locked, and ACTIVE (for changes).
     * Must be called inside a transaction.
     *
     * @throws ValidationException if the account is not ACTIVE
     */
    InvestmentAccount getOwnedActiveAccountForUpdate(String accountId, String userId);

    /**
     * ACTIVE account, row-locked, without an ownership check: for internal
     * service-to-service operations such as dividend and interest posting.
     */
    InvestmentAccount getActiveAccountForUpdate(String accountId);
}
