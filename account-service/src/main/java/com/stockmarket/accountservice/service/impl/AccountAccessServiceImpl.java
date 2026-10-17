package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.exception.AccountAccessDeniedException;
import com.stockmarket.accountservice.exception.ResourceNotFoundException;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.repository.InvestmentAccountRepository;
import com.stockmarket.accountservice.security.RequestUser;
import com.stockmarket.accountservice.service.AccountAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountAccessServiceImpl implements AccountAccessService {

    private final InvestmentAccountRepository accountRepository;

    /**
     * Reads are allowed to the owner and to ADMIN users; changes are owner-only.
     */
    @Override
    @Transactional(readOnly = true)
    public InvestmentAccount getOwnedAccount(String accountId, String userId) {

        InvestmentAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        if (!RequestUser.isAdmin()) {
            requireOwner(account, userId);
        }
        return account;
    }

    /**
     * MANDATORY: the row lock is only useful inside the caller's transaction.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public InvestmentAccount getOwnedActiveAccountForUpdate(String accountId, String userId) {

        InvestmentAccount account = lock(accountId);
        requireOwner(account, userId);
        requireActive(account);
        return account;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public InvestmentAccount getActiveAccountForUpdate(String accountId) {

        InvestmentAccount account = lock(accountId);
        requireActive(account);
        return account;
    }

    private InvestmentAccount lock(String accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }

    private void requireOwner(InvestmentAccount account, String userId) {
        if (!account.getUserId().equals(userId)) {
            log.warn("User {} tried to access account {}", userId, account.getId());
            throw AccountAccessDeniedException.forAccount(account.getId());
        }
    }

    private void requireActive(InvestmentAccount account) {
        if (!account.isActive()) {
            throw new ValidationException("Account " + account.getAccountNumber() + " is " + account.getStatus());
        }
    }
}
