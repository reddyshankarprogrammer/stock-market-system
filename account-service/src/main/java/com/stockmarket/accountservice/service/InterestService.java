package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.request.InterestRequest;
import com.stockmarket.accountservice.dto.response.InterestResponse;

import java.util.List;

/**
 * Interest on idle cash.
 */
public interface InterestService {

    /**
     * Internal: credits simple interest on the current balance for the period.
     * A period overlapping one already paid is rejected.
     */
    InterestResponse postInterest(String accountId, InterestRequest request);

    List<InterestResponse> getInterest(String accountId, String userId);
}
