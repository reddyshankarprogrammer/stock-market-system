package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.request.DividendRequest;
import com.stockmarket.accountservice.dto.response.DividendResponse;

import java.util.List;

/**
 * Cash dividends on held shares.
 */
public interface DividendService {

    /**
     * Internal: pays {@code dividendPerShare} on the shares currently held, withholds
     * dividend tax, and credits the net amount through the ledger.
     */
    DividendResponse postDividend(String accountId, DividendRequest request);

    List<DividendResponse> getDividends(String accountId, String userId);
}
