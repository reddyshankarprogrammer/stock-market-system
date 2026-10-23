package com.stockmarket.userservice.exception;

/**
 * Thrown when a signed-in user gives a wrong current password while changing it. Answered with 422 (a status of its
 * own, so callers need no message matching), never 401: the session is valid, and a 401 would make the web app end it.
 */
public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException() {
        super("Your current password is incorrect");
    }
}
