package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.AccountStatus;
import com.stockmarket.accountservice.enums.AccountType;
import com.stockmarket.accountservice.enums.BrokerName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An investment account.
 */
@Schema(description = "An investment account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    /**
     * Account id.
     */
    private String id;

    /**
     * Human-readable account number.
     */
    private String accountNumber;

    /**
     * Owner.
     */
    private String userId;

    /**
     * Display name.
     */
    private String accountName;

    /**
     * Account type.
     */
    private AccountType accountType;

    /**
     * Where the account is held.
     */
    private BrokerName brokerName;

    /**
     * Balance currency.
     */
    private String currency;

    /**
     * Free-text notes.
     */
    private String remarks;

    /**
     * Cash available.
     */
    private BigDecimal balance;

    /**
     * ACTIVE, SUSPENDED or CLOSED.
     */
    private AccountStatus status;

    /**
     * When the account was opened.
     */
    private LocalDateTime createdAt;

    /**
     * Last change.
     */
    private LocalDateTime updatedAt;

    /**
     * When the account was closed, if closed.
     */
    private LocalDateTime closedAt;
}
