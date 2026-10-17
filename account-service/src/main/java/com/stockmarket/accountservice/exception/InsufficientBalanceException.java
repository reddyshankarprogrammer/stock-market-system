package com.stockmarket.accountservice.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * The account balance does not cover a debit. Mapped to HTTP 422.
 */
@Getter
public class InsufficientBalanceException extends RuntimeException {

    private final BigDecimal required;
    private final BigDecimal available;

    public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
        super("Insufficient balance: required " + required.toPlainString()
                + ", available " + available.toPlainString());
        this.required = required;
        this.available = available;
    }
}
