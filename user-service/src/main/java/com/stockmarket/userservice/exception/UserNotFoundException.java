package com.stockmarket.userservice.exception;

/** The user behind a valid token no longer exists (for example deleted). */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User not found");
    }
}
