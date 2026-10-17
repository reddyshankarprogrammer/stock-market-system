package com.stockmarket.accountservice.exception;

/**
 * No price could be obtained from market-data-service. Mapped to HTTP 502.
 */
public class MarketDataUnavailableException extends RuntimeException {

    public MarketDataUnavailableException(String symbol, Throwable cause) {
        super("Market data unavailable for " + symbol, cause);
    }
}
