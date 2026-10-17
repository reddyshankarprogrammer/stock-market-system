package com.stockmarket.accountservice.dto.response;

import com.stockmarket.accountservice.enums.BrokerName;
import com.stockmarket.accountservice.enums.StatementFormat;
import com.stockmarket.accountservice.enums.StatementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An uploaded broker statement with its latest import result.
 */
@Schema(description = "An uploaded broker statement with its latest import result")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatementResponse {

    /**
     * Statement id.
     */
    private String id;

    /**
     * Account id.
     */
    private String accountId;

    /**
     * Owner.
     */
    private String userId;

    /**
     * Broker; selects the parser.
     */
    private BrokerName brokerName;

    /**
     * Name of the uploaded file.
     */
    private String originalFileName;

    /**
     * CSV, XLS or XLSX.
     */
    private StatementFormat fileFormat;

    /**
     * Size in bytes.
     */
    private long fileSize;

    /**
     * SHA-256 of the file.
     */
    private String checksumSha256;

    /**
     * Processing status.
     */
    private StatementStatus status;

    /**
     * First trade date in the file.
     */
    private LocalDate periodFrom;

    /**
     * Last trade date in the file.
     */
    private LocalDate periodTo;

    /**
     * Upload time.
     */
    private LocalDateTime uploadedAt;

    /**
     * Last processing time.
     */
    private LocalDateTime processedAt;

    /**
     * Result of the latest import attempt.
     */
    private StatementImportHistoryResponse latestImport;
}
