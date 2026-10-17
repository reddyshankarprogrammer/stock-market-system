package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.ImportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of one import attempt.
 */
@Schema(description = "Result of one import attempt")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementImportHistoryResponse {

    /**
     * Attempt id.
     */
    private String id;

    /**
     * Statement id.
     */
    private String statementId;

    /**
     * 1 for the first import, then 2, 3, ... per reprocess.
     */
    private int attemptNumber;

    /**
     * SUCCESS, PARTIAL or FAILED.
     */
    private ImportStatus status;

    /**
     * Parser used.
     */
    private String parserName;

    /**
     * Start.
     */
    private LocalDateTime startedAt;

    /**
     * End.
     */
    private LocalDateTime finishedAt;

    /**
     * Data rows read.
     */
    private int rowsRead;

    /**
     * Rows skipped (see warnings).
     */
    private int rowsSkipped;

    /**
     * Trades stored.
     */
    private int tradesImported;

    /**
     * Trades skipped as already imported.
     */
    private int duplicateTrades;

    /**
     * Charges stored.
     */
    private int chargesImported;

    /**
     * Taxes stored.
     */
    private int taxesImported;

    /**
     * Realized P&L reported by the broker.
     */
    private BigDecimal reportedRealizedPnl;

    /**
     * Unrealized P&L reported by the broker.
     */
    private BigDecimal reportedUnrealizedPnl;

    /**
     * Charges reported by the broker.
     */
    private BigDecimal reportedCharges;

    /**
     * Taxes reported by the broker.
     */
    private BigDecimal reportedTaxes;

    /**
     * Net P&L reported by the broker.
     */
    private BigDecimal reportedNetPnl;

    /**
     * Skipped rows, unmapped labels, duplicates.
     */
    private List<String> warnings;

    /**
     * Why the attempt failed.
     */
    private String errorMessage;
}
