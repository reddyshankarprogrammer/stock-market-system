package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.LedgerCategory;
import com.stockmarket.accountservice.enums.LedgerEntryType;
import com.stockmarket.accountservice.enums.ReferenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One ledger entry with its running balance.
 */
@Schema(description = "One ledger entry with its running balance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerResponse {

    /**
     * Entry id.
     */
    private String id;

    /**
     * Position in the account's ledger: 1, 2, 3, ...
     */
    private long entryNumber;

    /**
     * DEBIT or CREDIT.
     */
    private LedgerEntryType entryType;

    /**
     * Business reason.
     */
    private LedgerCategory category;

    /**
     * Amount (always positive).
     */
    private BigDecimal amount;

    /**
     * Running balance after this entry.
     */
    private BigDecimal balanceAfter;

    /**
     * Kind of record that caused the entry.
     */
    private ReferenceType referenceType;

    /**
     * Id of that record.
     */
    private String referenceId;

    /**
     * Description.
     */
    private String description;

    /**
     * When the entry was posted.
     */
    private LocalDateTime transactionDate;
}
