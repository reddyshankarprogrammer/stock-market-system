package com.stockmarket.accountservice.exception;

/**
 * A requested record does not exist. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
