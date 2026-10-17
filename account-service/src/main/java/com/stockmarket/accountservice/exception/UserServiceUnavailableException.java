package com.stockmarket.accountservice.exception;

/**
 * user-service could not be reached or answered with an unexpected error. Mapped to HTTP 502.
 */
public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(Throwable cause) {
        super("User service unavailable", cause);
    }
}
