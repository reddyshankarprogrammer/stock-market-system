package com.stockmarket.accountservice.statement.parser;

/**
 * The file could not be read or does not contain anything this parser recognizes.
 * Recorded on the import history as a FAILED attempt.
 */
public class StatementParseException extends RuntimeException {

    public StatementParseException(String message) {
        super(message);
    }

    public StatementParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
