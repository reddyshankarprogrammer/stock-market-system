package com.stockmarket.userservice.exception;

/**
 * Thrown when a password-reset token is unknown, already used, expired, or belongs to a blocked or missing user.
 * One message for all cases, so callers learn nothing about which it was.
 */
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("This reset link is invalid or has expired");
    }
}
