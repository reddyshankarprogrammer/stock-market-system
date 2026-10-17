package com.stockmarket.accountservice.statement.service;

import com.stockmarket.accountservice.entity.AccountCharge;
import com.stockmarket.accountservice.entity.AccountStatement;
import com.stockmarket.accountservice.entity.AccountTax;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.entity.StatementImportHistory;
import com.stockmarket.accountservice.entity.Trade;
import com.stockmarket.accountservice.enums.AuditAction;
import com.stockmarket.accountservice.enums.ImportStatus;
import com.stockmarket.accountservice.enums.RecordSource;
import com.stockmarket.accountservice.enums.StatementStatus;
import com.stockmarket.accountservice.enums.TradeKind;
import com.stockmarket.accountservice.enums.TradeSide;
import com.stockmarket.accountservice.exception.ResourceNotFoundException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AccountChargeRepository;
import com.stockmarket.accountservice.repository.AccountStatementRepository;
import com.stockmarket.accountservice.repository.AccountTaxRepository;
import com.stockmarket.accountservice.repository.StatementImportHistoryRepository;
import com.stockmarket.accountservice.repository.TradeRepository;
import com.stockmarket.accountservice.service.AuditService;
import com.stockmarket.accountservice.service.HoldingService;
import com.stockmarket.accountservice.service.impl.TrackedChargeRecorder;
import com.stockmarket.accountservice.service.model.Money;
import com.stockmarket.accountservice.statement.parser.BrokerImportProperties;
import com.stockmarket.accountservice.statement.parser.ParsedStatement;
import com.stockmarket.accountservice.statement.parser.ParserFactory;
import com.stockmarket.accountservice.statement.parser.StatementParser;
import com.stockmarket.accountservice.statement.parser.TabularData;
import com.stockmarket.accountservice.statement.parser.TabularDataReader;
import com.stockmarket.accountservice.statement.storage.StatementStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Import pipeline:
 * <ol>
 *   <li>tx 1: mark the statement PROCESSING;</li>
 *   <li>no tx: read the file and parse it (file I/O never holds a database connection);</li>
 *   <li>tx 2: remove the statement's earlier import, store trades/charges/taxes, rebuild
 *       holdings, write the history row and audit entry;</li>
 *   <li>on any failure, tx 3 records a FAILED attempt instead.</li>
 * </ol>
 */
@Slf4j
@Service
public class StatementImportServiceImpl implements StatementImportService {

    private final AccountStatementRepository statementRepository;
    private final StatementImportHistoryRepository historyRepository;
    private final TradeRepository tradeRepository;
    private final AccountChargeRepository chargeRepository;
    private final AccountTaxRepository taxRepository;
    private final HoldingService holdingService;
    private final TrackedChargeRecorder trackedChargeRecorder;
    private final AuditService auditService;
    private final StatementStorageService storageService;
    private final TabularDataReader reader;
    private final ParserFactory parserFactory;
    private final BrokerImportProperties importProperties;
    private final AccountMapper mapper;
    private final TransactionTemplate transactionTemplate;

    public StatementImportServiceImpl(AccountStatementRepository statementRepository,
                                      StatementImportHistoryRepository historyRepository,
                                      TradeRepository tradeRepository,
                                      AccountChargeRepository chargeRepository,
                                      AccountTaxRepository taxRepository,
                                      HoldingService holdingService,
                                      TrackedChargeRecorder trackedChargeRecorder,
                                      AuditService auditService,
                                      StatementStorageService storageService,
                                      TabularDataReader reader,
                                      ParserFactory parserFactory,
                                      BrokerImportProperties importProperties,
                                      AccountMapper mapper,
                                      PlatformTransactionManager transactionManager) {
        this.statementRepository = statementRepository;
        this.historyRepository = historyRepository;
        this.tradeRepository = tradeRepository;
        this.chargeRepository = chargeRepository;
        this.taxRepository = taxRepository;
        this.holdingService = holdingService;
        this.trackedChargeRecorder = trackedChargeRecorder;
        this.auditService = auditService;
        this.storageService = storageService;
        this.reader = reader;
        this.parserFactory = parserFactory;
        this.importProperties = importProperties;
        this.mapper = mapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public StatementImportHistory process(String statementId) {

        LocalDateTime startedAt = LocalDateTime.now();
        AccountStatement statement = transactionTemplate.execute(status -> {
            AccountStatement s = statementRepository.findById(statementId)
                    .orElseThrow(() -> new ResourceNotFoundException("Statement", statementId));
            s.setStatus(StatementStatus.PROCESSING);
            return statementRepository.save(s);
        });

        StatementParser parser = parserFactory.getParser(statement.getBrokerName());
        String parserName = parser.getClass().getSimpleName();

        ParsedStatement parsed;
        try (InputStream in = storageService.open(statement.getStoragePath())) {
            TabularData data = reader.read(in, statement.getFileFormat(), importProperties.getMaxRows());
            parsed = parser.parse(data);
        } catch (Exception e) {
            log.warn("Statement {} could not be parsed: {}", statementId, e.getMessage());
            return recordFailure(statementId, parserName, startedAt, e);
        }

        try {
            return transactionTemplate.execute(status -> persist(statementId, parserName, startedAt, parsed));
        } catch (RuntimeException e) {
            log.error("Statement {} could not be imported", statementId, e);
            return recordFailure(statementId, parserName, startedAt, e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeImportedData(String statementId) {

        List<String> tradeIds = tradeRepository.findIdsByStatementId(statementId);
        if (!tradeIds.isEmpty()) {
            chargeRepository.deleteByTradeIdIn(tradeIds);
            taxRepository.deleteByTradeIdIn(tradeIds);
        }
        chargeRepository.deleteByStatementId(statementId);
        taxRepository.deleteByStatementId(statementId);
        tradeRepository.deleteByStatementId(statementId);
    }

    private StatementImportHistory persist(String statementId, String parserName, LocalDateTime startedAt,
                                           ParsedStatement parsed) {

        AccountStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement", statementId));
        InvestmentAccount account = statement.getAccount();

        removeImportedData(statementId);
        tradeRepository.flush();

        List<String> warnings = new ArrayList<>(parsed.warnings());
        Set<String> seenIds = new HashSet<>();
        int imported = 0;
        int duplicates = 0;
        int charges = 0;
        int taxes = 0;

        for (ParsedStatement.Trade row : parsed.trades()) {
            if (row.externalId() != null
                    && (!seenIds.add(row.externalId()) || tradeRepository.existsByAccountIdAndExternalTradeId(account.getId(), row.externalId()))) {
                duplicates++;
                warnings.add("Row " + row.rowNumber() + ": trade " + row.externalId() + " already imported, skipped");
                continue;
            }
            Trade trade = tradeRepository.save(toTrade(account, statement, row));
            int[] written = trackedChargeRecorder.record(account, trade, RecordSource.IMPORT, statement);
            charges += written[0];
            taxes += written[1];
            imported++;
        }

        LocalDate chargeDate = parsed.periodTo() != null ? parsed.periodTo() : statement.getUploadedAt().toLocalDate();
        for (ParsedStatement.Charge charge : parsed.charges()) {
            chargeRepository.save(AccountCharge.builder()
                    .account(account)
                    .userId(account.getUserId())
                    .statement(statement)
                    .chargeType(charge.type())
                    .amount(Money.amount(charge.amount()))
                    .chargeDate(chargeDate)
                    .source(RecordSource.IMPORT)
                    .description(truncate(charge.label(), 255))
                    .build());
            charges++;
        }
        for (ParsedStatement.Tax tax : parsed.taxes()) {
            taxRepository.save(AccountTax.builder()
                    .account(account)
                    .statement(statement)
                    .taxType(tax.type())
                    .source(RecordSource.IMPORT)
                    .taxableAmount(Money.zero())
                    .taxAmount(Money.amount(tax.amount()))
                    .chargedAt(chargeDate.atTime(23, 59))
                    .description(truncate(tax.label(), 255))
                    .build());
            taxes++;
        }

        warnings.addAll(holdingService.rebuildHoldings(account));

        boolean clean = parsed.rowsSkipped() == 0 && warnings.isEmpty();
        ImportStatus importStatus = imported + charges + taxes == 0 && !parsed.trades().isEmpty()
                ? ImportStatus.FAILED
                : clean ? ImportStatus.SUCCESS : ImportStatus.PARTIAL;

        ParsedStatement.PnlSummary summary = parsed.summary();
        StatementImportHistory history = historyRepository.save(StatementImportHistory.builder()
                .statement(statement)
                .attemptNumber(historyRepository.findMaxAttemptNumber(statementId) + 1)
                .status(importStatus)
                .parserName(parserName)
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now())
                .rowsRead(parsed.rowsRead())
                .rowsSkipped(parsed.rowsSkipped())
                .tradesImported(imported)
                .duplicateTrades(duplicates)
                .chargesImported(charges)
                .taxesImported(taxes)
                .reportedRealizedPnl(scale(summary.realizedPnl()))
                .reportedUnrealizedPnl(scale(summary.unrealizedPnl()))
                .reportedCharges(scale(summary.charges()))
                .reportedTaxes(scale(summary.taxes()))
                .reportedNetPnl(scale(summary.netPnl()))
                .warnings(warnings.isEmpty() ? null : String.join("\n", warnings))
                .build());

        statement.setStatus(switch (importStatus) {
            case SUCCESS -> StatementStatus.PROCESSED;
            case PARTIAL -> StatementStatus.PARTIALLY_PROCESSED;
            case FAILED -> StatementStatus.FAILED;
        });
        statement.setPeriodFrom(parsed.periodFrom());
        statement.setPeriodTo(parsed.periodTo());
        statement.setProcessedAt(LocalDateTime.now());
        statementRepository.save(statement);

        auditService.logAccountAction(account.getId(), AuditAction.STATEMENT_PROCESS,
                "Imported statement " + statementId + ": " + imported + " trades, " + charges + " charges, "
                        + taxes + " taxes (" + importStatus + ")",
                null, mapper.toImportHistoryResponse(history));

        log.info("Statement {} imported: {} trades, {} duplicates, {} charges, {} taxes, {} warnings",
                statementId, imported, duplicates, charges, taxes, warnings.size());
        return history;
    }

    private StatementImportHistory recordFailure(String statementId, String parserName,
                                                 LocalDateTime startedAt, Exception error) {
        return transactionTemplate.execute(status -> {
            AccountStatement statement = statementRepository.findById(statementId)
                    .orElseThrow(() -> new ResourceNotFoundException("Statement", statementId));
            statement.setStatus(StatementStatus.FAILED);
            statement.setProcessedAt(LocalDateTime.now());
            statementRepository.save(statement);

            StatementImportHistory history = historyRepository.save(StatementImportHistory.builder()
                    .statement(statement)
                    .attemptNumber(historyRepository.findMaxAttemptNumber(statementId) + 1)
                    .status(ImportStatus.FAILED)
                    .parserName(parserName)
                    .startedAt(startedAt)
                    .finishedAt(LocalDateTime.now())
                    .errorMessage(truncate(error.getMessage() == null ? error.toString() : error.getMessage(), 2000))
                    .build());

            auditService.logAccountAction(statement.getAccount().getId(), AuditAction.STATEMENT_PROCESS,
                    "Import of statement " + statementId + " failed: " + history.getErrorMessage(),
                    null, mapper.toImportHistoryResponse(history));
            return history;
        });
    }

    /**
     * Builds the trade entity for an imported row. Realized profit of a closed position is the
     * broker's P&amp;L minus the row's fees; for SELL executions it is computed by the holdings rebuild.
     */
    private static Trade toTrade(InvestmentAccount account, AccountStatement statement, ParsedStatement.Trade row) {

        BigDecimal quantity = row.quantity().setScale(8, RoundingMode.HALF_UP);
        BigDecimal brokerage = Money.amount(row.brokerage());
        BigDecimal otherFee = Money.amount(row.otherFee());
        BigDecimal fees = brokerage.add(otherFee);
        BigDecimal tax = Money.amount(row.tax());

        Trade.TradeBuilder builder = Trade.builder()
                .account(account)
                .userId(account.getUserId())
                .statement(statement)
                .source(RecordSource.IMPORT)
                .tradeKind(row.kind())
                .side(row.side())
                .symbol(row.symbol())
                .exchange(row.exchange())
                .quantity(quantity)
                .brokerage(brokerage)
                .otherFee(otherFee)
                .feeAmount(fees)
                .taxAmount(tax)
                .tradeDate(row.tradeDate())
                .externalTradeId(row.externalId() == null ? null : truncate(row.externalId(), 100));

        if (row.kind() == TradeKind.CLOSED_POSITION) {
            BigDecimal buy = row.buyPrice().setScale(8, RoundingMode.HALF_UP);
            BigDecimal sell = row.sellPrice().setScale(8, RoundingMode.HALF_UP);
            BigDecimal closing = row.side() == TradeSide.BUY ? sell : buy;
            BigDecimal realized = Money.amount(row.realizedPnl().subtract(fees));
            return builder.price(closing).buyPrice(buy).sellPrice(sell).averageCostAtTrade(buy)
                    .grossAmount(Money.amount(closing.multiply(quantity)))
                    .realizedProfit(realized).netAmount(realized.subtract(tax))
                    .build();
        }

        BigDecimal price = row.price().setScale(8, RoundingMode.HALF_UP);
        BigDecimal gross = Money.amount(price.multiply(quantity));
        if (row.side() == TradeSide.BUY) {
            return builder.price(price).buyPrice(price).grossAmount(gross).netAmount(gross.add(fees).add(tax)).build();
        }
        return builder.price(price).sellPrice(price).grossAmount(gross).netAmount(gross.subtract(fees).subtract(tax)).build();
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : Money.amount(value);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
