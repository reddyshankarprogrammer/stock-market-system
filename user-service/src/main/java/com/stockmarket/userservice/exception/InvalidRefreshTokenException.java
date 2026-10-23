package com.stockmarket.userservice.exception;

/**
 * Thrown when a refresh token cannot be exchanged: bad signature, expired, not a refresh token, unknown user,
 * or a user whose status blocks sign-in. One message for all cases, so callers learn nothing about which it was.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
