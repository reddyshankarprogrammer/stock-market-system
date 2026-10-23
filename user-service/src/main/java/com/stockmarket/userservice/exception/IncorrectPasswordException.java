package com.stockmarket.userservice.exception;

/**
 * Thrown when a signed-in user gives a wrong current password while changing it. Answered with 400, never 401: the
 * session is valid, and a 401 would make the web app end it.
 */
public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException() {
        super("Your current password is incorrect");
    }
}
