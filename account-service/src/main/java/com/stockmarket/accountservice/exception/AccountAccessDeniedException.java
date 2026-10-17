package com.stockmarket.accountservice.exception;

/**
 * The caller ({@code X-User-Id} / {@code X-User-Role}) may not access the requested data
 * or endpoint. Mapped to HTTP 403.
 */
public class AccountAccessDeniedException extends RuntimeException {

    private AccountAccessDeniedException(String message) {
        super(message);
    }

    public static AccountAccessDeniedException forAccount(String accountId) {
        return new AccountAccessDeniedException("Access denied to account: " + accountId);
    }

    public static AccountAccessDeniedException forUser(String userId) {
        return new AccountAccessDeniedException("Access denied to accounts of user: " + userId);
    }

    public static AccountAccessDeniedException adminOnly() {
        return new AccountAccessDeniedException("This operation requires the ADMIN role");
    }
}
