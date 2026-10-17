package com.stockmarket.accountservice.statement.controller;

import com.stockmarket.accountservice.controller.ApiHeaders;
import com.stockmarket.accountservice.dto.response.AccountStatementResponse;
import com.stockmarket.accountservice.dto.response.StatementImportHistoryResponse;
import com.stockmarket.accountservice.statement.service.AccountStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Broker statement upload and import.
 */
@Tag(name = "Statements (import)", description = "Upload broker statements (xlsx, xls, csv) and manage their imports")
@RestController
@RequiredArgsConstructor
public class AccountStatementController {

    private final AccountStatementService statementService;

    @Operation(summary = "Upload a broker statement",
            description = "Multipart field 'file': xlsx, xls or csv, at most 20 MB. The account must be an ACTIVE "
                    + "GROWW, ZERODHA, BINANCE or EXNESS account of the caller. The file is stored and imported "
                    + "right away; the response includes the import result.")
    @ApiResponse(responseCode = "201", description = "Stored and imported (check latestImport.status)")
    @ApiResponse(responseCode = "400", description = "Wrong file type, content, size or account")
    @ApiResponse(responseCode = "409", description = "Same file already uploaded to this account")
    @PostMapping(path = "/api/v1/accounts/{accountId}/statements/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccountStatementResponse> upload(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId,
            @RequestPart("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED).body(statementService.upload(accountId, userId, file));
    }

    @Operation(summary = "Statements of an account", description = "Newest first, each with its latest import result.")
    @GetMapping("/api/v1/accounts/{accountId}/statements")
    public ResponseEntity<List<AccountStatementResponse>> getStatements(
            @PathVariable String accountId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(statementService.getStatements(accountId, userId));
    }

    @Operation(summary = "Get a statement")
    @GetMapping("/api/v1/statements/{statementId}")
    public ResponseEntity<AccountStatementResponse> getStatement(
            @PathVariable String statementId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(statementService.getStatement(statementId, userId));
    }

    @Operation(summary = "Download the original file")
    @GetMapping("/api/v1/statements/{statementId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable String statementId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        AccountStatementService.StatementFile file = statementService.download(statementId, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }

    @Operation(summary = "Import history", description = "Every import attempt of the statement, newest first.")
    @GetMapping("/api/v1/statements/{statementId}/imports")
    public ResponseEntity<List<StatementImportHistoryResponse>> getImportHistory(
            @PathVariable String statementId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(statementService.getImportHistory(statementId, userId));
    }

    @Operation(summary = "Reprocess a statement",
            description = "Imports the stored file again (for example after a column mapping change). "
                    + "What it imported before is replaced, never duplicated.")
    @PostMapping("/api/v1/statements/{statementId}/reprocess")
    public ResponseEntity<AccountStatementResponse> reprocess(
            @PathVariable String statementId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        return ResponseEntity.ok(statementService.reprocess(statementId, userId));
    }

    @Operation(summary = "Delete a statement",
            description = "Deletes the file, the statement and every trade, charge and tax imported from it; "
                    + "holdings are rebuilt.")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/api/v1/statements/{statementId}")
    public ResponseEntity<Void> delete(
            @PathVariable String statementId,
            @Parameter(description = ApiHeaders.USER_ID_DESCRIPTION) @RequestHeader(ApiHeaders.USER_ID) String userId) {

        statementService.delete(statementId, userId);
        return ResponseEntity.noContent().build();
    }
}
