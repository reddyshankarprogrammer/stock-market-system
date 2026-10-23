package com.stockmarket.userservice.exception;

/**
 * Thrown for a failed login. The same message and status (401) for an unknown email and for a wrong password, so the
 * answer cannot be used to find out which emails are registered.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid Credentials");
    }
}
