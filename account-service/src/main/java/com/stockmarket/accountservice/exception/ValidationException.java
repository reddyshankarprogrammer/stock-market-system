package com.stockmarket.accountservice.exception;

/**
 * A business rule rejected the request, for example selling more shares than held
 * or trading on a closed account. Mapped to HTTP 400.
 *
 * Field-level input errors are reported by Bean Validation instead.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
