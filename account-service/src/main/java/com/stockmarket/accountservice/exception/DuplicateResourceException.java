package com.stockmarket.accountservice.exception;

import lombok.Getter;

/**
 * The resource already exists, for example the same statement file uploaded twice.
 * Mapped to HTTP 409 with the id of the existing resource.
 */
@Getter
public class DuplicateResourceException extends RuntimeException {

    private final String existingId;

    public DuplicateResourceException(String message, String existingId) {
        super(message);
        this.existingId = existingId;
    }
}
