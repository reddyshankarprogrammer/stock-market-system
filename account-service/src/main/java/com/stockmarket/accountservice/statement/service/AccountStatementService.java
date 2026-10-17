package com.stockmarket.accountservice.statement.service;

import com.stockmarket.accountservice.dto.response.AccountStatementResponse;
import com.stockmarket.accountservice.dto.response.StatementImportHistoryResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Broker statement files of tracking accounts: upload (stored, then imported), view,
 * download, reprocess and delete.
 */
public interface AccountStatementService {

    /**
     * Validates (owner, ACTIVE tracking account with a parser, xlsx/xls/csv, at most 20 MB,
     * not uploaded before), stores the file, then imports it.
     */
    AccountStatementResponse upload(String accountId, String userId, MultipartFile file);

    List<AccountStatementResponse> getStatements(String accountId, String userId);

    AccountStatementResponse getStatement(String statementId, String userId);

    List<StatementImportHistoryResponse> getImportHistory(String statementId, String userId);

    /**
     * Imports the stored file again (new attempt), replacing what it imported before.
     */
    AccountStatementResponse reprocess(String statementId, String userId);

    /**
     * Deletes the statement, everything imported from it and its file; holdings are rebuilt.
     */
    void delete(String statementId, String userId);

    /**
     * The stored file for download.
     */
    StatementFile download(String statementId, String userId);

    /**
     * A stored statement file and how to serve it.
     */
    record StatementFile(Resource resource, String fileName, String contentType) {
    }
}
