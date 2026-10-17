package com.stockmarket.accountservice.statement.service;

import com.stockmarket.accountservice.dto.response.AccountStatementResponse;
import com.stockmarket.accountservice.dto.response.StatementImportHistoryResponse;
import com.stockmarket.accountservice.entity.AccountStatement;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.enums.AuditAction;
import com.stockmarket.accountservice.enums.StatementFormat;
import com.stockmarket.accountservice.enums.StatementStatus;
import com.stockmarket.accountservice.exception.AccountAccessDeniedException;
import com.stockmarket.accountservice.exception.DuplicateResourceException;
import com.stockmarket.accountservice.exception.ResourceNotFoundException;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AccountStatementRepository;
import com.stockmarket.accountservice.repository.InvestmentAccountRepository;
import com.stockmarket.accountservice.repository.StatementImportHistoryRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.AuditService;
import com.stockmarket.accountservice.service.HoldingService;
import com.stockmarket.accountservice.statement.parser.ParserFactory;
import com.stockmarket.accountservice.statement.storage.StatementStorageProperties;
import com.stockmarket.accountservice.statement.storage.StatementStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Statement lifecycle. Upload and import are separate transactions: the file and its row
 * are committed first, so a failing import leaves a FAILED statement that can be reprocessed.
 */
@Slf4j
@Service
public class AccountStatementServiceImpl implements AccountStatementService {

    private static final Map<String, StatementFormat> EXTENSIONS = Map.of(
            "csv", StatementFormat.CSV, "xls", StatementFormat.XLS, "xlsx", StatementFormat.XLSX);

    private final AccountStatementRepository statementRepository;
    private final StatementImportHistoryRepository historyRepository;
    private final InvestmentAccountRepository accountRepository;
    private final AccountAccessService accountAccessService;
    private final StatementImportService importService;
    private final StatementStorageService storageService;
    private final StatementStorageProperties storageProperties;
    private final ParserFactory parserFactory;
    private final HoldingService holdingService;
    private final AuditService auditService;
    private final AccountMapper mapper;
    private final TransactionTemplate transactionTemplate;

    public AccountStatementServiceImpl(AccountStatementRepository statementRepository,
                                       StatementImportHistoryRepository historyRepository,
                                       InvestmentAccountRepository accountRepository,
                                       AccountAccessService accountAccessService,
                                       StatementImportService importService,
                                       StatementStorageService storageService,
                                       StatementStorageProperties storageProperties,
                                       ParserFactory parserFactory,
                                       HoldingService holdingService,
                                       AuditService auditService,
                                       AccountMapper mapper,
                                       PlatformTransactionManager transactionManager) {
        this.statementRepository = statementRepository;
        this.historyRepository = historyRepository;
        this.accountRepository = accountRepository;
        this.accountAccessService = accountAccessService;
        this.importService = importService;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.parserFactory = parserFactory;
        this.holdingService = holdingService;
        this.auditService = auditService;
        this.mapper = mapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public AccountStatementResponse upload(String accountId, String userId, MultipartFile file) {

        StatementFormat format = validateFile(file);
        String checksum = sha256(file);

        String statementId = transactionTemplate.execute(status -> {
            InvestmentAccount account = requireOwnedImportableAccount(accountId, userId);

            statementRepository.findByAccountIdAndChecksumSha256(accountId, checksum).ifPresent(existing -> {
                throw new DuplicateResourceException("This file was already uploaded as statement " + existing.getId(),
                        existing.getId());
            });

            AccountStatement statement = statementRepository.save(AccountStatement.builder()
                    .account(account)
                    .userId(account.getUserId())
                    .brokerName(account.getBrokerName())
                    .originalFileName(cleanFileName(file.getOriginalFilename()))
                    .storagePath("pending")
                    .contentType(file.getContentType())
                    .fileFormat(format)
                    .fileSize(file.getSize())
                    .checksumSha256(checksum)
                    .status(StatementStatus.UPLOADED)
                    .uploadedAt(LocalDateTime.now())
                    .build());

            try (InputStream in = file.getInputStream()) {
                statement.setStoragePath(storageService.store(accountId, statement.getId(),
                        format.name().toLowerCase(Locale.ROOT), in));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            statementRepository.save(statement);

            auditService.logAccountAction(accountId, AuditAction.STATEMENT_UPLOAD,
                    "Uploaded " + statement.getOriginalFileName() + " (" + format + ", " + file.getSize() + " bytes)",
                    Map.of("fileName", statement.getOriginalFileName(), "size", file.getSize(), "format", format),
                    mapper.toStatementResponse(statement));
            return statement.getId();
        });

        importService.process(statementId);
        return getStatement(statementId, userId);
    }

    @Override
    public List<AccountStatementResponse> getStatements(String accountId, String userId) {
        return transactionTemplate.execute(status -> {
            accountAccessService.getOwnedAccount(accountId, userId);
            return statementRepository.findByAccountIdOrderByUploadedAtDesc(accountId).stream()
                    .map(this::toResponse)
                    .toList();
        });
    }

    @Override
    public AccountStatementResponse getStatement(String statementId, String userId) {
        return transactionTemplate.execute(status -> toResponse(readable(statementId, userId)));
    }

    @Override
    public List<StatementImportHistoryResponse> getImportHistory(String statementId, String userId) {
        return transactionTemplate.execute(status -> {
            readable(statementId, userId);
            return mapper.toImportHistoryResponses(historyRepository.findByStatementIdOrderByAttemptNumberDesc(statementId));
        });
    }

    @Override
    public AccountStatementResponse reprocess(String statementId, String userId) {

        transactionTemplate.executeWithoutResult(status -> {
            AccountStatement statement = owned(statementId, userId);
            if (!statement.getAccount().isActive()) {
                throw new ValidationException("Account " + statement.getAccount().getAccountNumber() + " is not active");
            }
        });
        importService.process(statementId);
        return getStatement(statementId, userId);
    }

    @Override
    public void delete(String statementId, String userId) {

        String storagePath = transactionTemplate.execute(status -> {
            AccountStatement statement = owned(statementId, userId);
            InvestmentAccount account = accountAccessService.getOwnedActiveAccountForUpdate(
                    statement.getAccount().getId(), userId);

            importService.removeImportedData(statementId);
            historyRepository.deleteByStatementId(statementId);
            statementRepository.delete(statement);
            statementRepository.flush();
            List<String> warnings = holdingService.rebuildHoldings(account);

            auditService.logAccountAction(account.getId(), AuditAction.STATEMENT_DELETE,
                    "Deleted statement " + statementId + " (" + statement.getOriginalFileName() + ") and its imported records",
                    null, warnings.isEmpty() ? null : Map.of("warnings", warnings));
            return statement.getStoragePath();
        });

        // The file goes only after the database delete has committed.
        storageService.delete(storagePath);
    }

    @Override
    public StatementFile download(String statementId, String userId) {
        AccountStatement statement = transactionTemplate.execute(status -> readable(statementId, userId));
        String contentType = switch (statement.getFileFormat()) {
            case CSV -> "text/csv";
            case XLS -> "application/vnd.ms-excel";
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
        return new StatementFile(storageService.load(statement.getStoragePath()), statement.getOriginalFileName(), contentType);
    }

    private AccountStatementResponse toResponse(AccountStatement statement) {
        AccountStatementResponse response = mapper.toStatementResponse(statement);
        historyRepository.findFirstByStatementIdOrderByAttemptNumberDesc(statement.getId())
                .map(mapper::toImportHistoryResponse)
                .ifPresent(response::setLatestImport);
        return response;
    }

    /**
     * Statement whose account the caller may read (owner or ADMIN).
     */
    private AccountStatement readable(String statementId, String userId) {
        AccountStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement", statementId));
        accountAccessService.getOwnedAccount(statement.getAccount().getId(), userId);
        return statement;
    }

    /**
     * Statement owned by the caller (changes are owner-only, also for ADMIN).
     */
    private AccountStatement owned(String statementId, String userId) {
        AccountStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement", statementId));
        if (!statement.getUserId().equals(userId)) {
            throw AccountAccessDeniedException.forAccount(statement.getAccount().getId());
        }
        return statement;
    }

    private InvestmentAccount requireOwnedImportableAccount(String accountId, String userId) {

        InvestmentAccount account = accountRepository.findByUserIdAndId(userId, accountId)
                .orElseThrow(() -> accountRepository.existsById(accountId)
                        ? AccountAccessDeniedException.forAccount(accountId)
                        : new ResourceNotFoundException("Account", accountId));

        if (!account.isActive()) {
            throw new ValidationException("Account " + account.getAccountNumber() + " is " + account.getStatus());
        }
        if (!account.getBrokerName().supportsStatementImport() || !parserFactory.supports(account.getBrokerName())) {
            throw new ValidationException("Statements cannot be imported for " + account.getBrokerName()
                    + " accounts; supported brokers: GROWW, ZERODHA, BINANCE, EXNESS");
        }
        return account;
    }

    /**
     * Not empty, within the size limit, a known extension, and content that matches it
     * (XLSX is a ZIP, XLS an OLE2 document, CSV text without NUL bytes).
     */
    private StatementFormat validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ValidationException("The file is empty");
        }
        if (file.getSize() > storageProperties.getMaxFileSize().toBytes()) {
            throw new ValidationException("The file is larger than " + storageProperties.getMaxFileSize().toMegabytes() + " MB");
        }

        String name = cleanFileName(file.getOriginalFilename());
        int dot = name.lastIndexOf('.');
        StatementFormat format = dot < 0 ? null : EXTENSIONS.get(name.substring(dot + 1).toLowerCase(Locale.ROOT));
        if (format == null) {
            throw new ValidationException("Unsupported file type '" + name + "'; upload xlsx, xls or csv");
        }

        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(512);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        boolean matches = switch (format) {
            case XLSX -> head.length >= 4 && head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4;
            case XLS -> head.length >= 8 && (head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF
                    && (head[2] & 0xFF) == 0x11 && (head[3] & 0xFF) == 0xE0;
            case CSV -> {
                boolean text = true;
                for (byte b : head) {
                    if (b == 0) {
                        text = false;
                        break;
                    }
                }
                yield text;
            }
        };
        if (!matches) {
            throw new ValidationException("The file content is not a valid " + format + " file");
        }
        return format;
    }

    private static String sha256(MultipartFile file) {
        try (DigestInputStream in = new DigestInputStream(file.getInputStream(), MessageDigest.getInstance("SHA-256"))) {
            in.transferTo(java.io.OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(in.getMessageDigest().digest());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * File name without any directory part, at most 255 characters.
     */
    private static String cleanFileName(String original) {
        String name = original == null ? "statement" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) {
            name = "statement";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
