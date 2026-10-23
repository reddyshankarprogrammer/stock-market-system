package com.stockmarket.userservice.exception;

/**
 * Thrown when the password is correct but the account's status blocks sign-in (suspended, banned, locked, ...).
 * Only raised after the password check, so someone guessing passwords never learns an account's status.
 */
public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException() {
        super("This account is not active");
    }
}
