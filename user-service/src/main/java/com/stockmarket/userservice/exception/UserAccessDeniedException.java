package com.stockmarket.userservice.exception;

/**
 * Thrown when the authenticated user (X-User-Id) acts on another user's account.
 */
public class UserAccessDeniedException extends RuntimeException {

    public UserAccessDeniedException(String userId) {
        super("Access denied to user: " + userId);
    }
}
