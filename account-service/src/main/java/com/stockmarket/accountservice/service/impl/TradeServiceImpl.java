package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.client.MarketDataClient;
import com.stockmarket.accountservice.dto.request.BuyTradeRequest;
import com.stockmarket.accountservice.dto.request.ManualTradeRequest;
import com.stockmarket.accountservice.dto.request.SellTradeRequest;
import com.stockmarket.accountservice.dto.response.TradeResponse;
import com.stockmarket.accountservice.entity.Holding;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.entity.Trade;
import com.stockmarket.accountservice.enums.AuditAction;
import com.stockmarket.accountservice.enums.FeeType;
import com.stockmarket.accountservice.enums.LedgerCategory;
import com.stockmarket.accountservice.enums.RecordSource;
import com.stockmarket.accountservice.enums.ReferenceType;
import com.stockmarket.accountservice.enums.TradeKind;
import com.stockmarket.accountservice.enums.TradeSide;
import com.stockmarket.accountservice.event.TradeExecutedEvent;
import com.stockmarket.accountservice.exception.DuplicateResourceException;
import com.stockmarket.accountservice.exception.InsufficientBalanceException;
import com.stockmarket.accountservice.exception.ResourceNotFoundException;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.HoldingRepository;
import com.stockmarket.accountservice.repository.TradeRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.AuditService;
import com.stockmarket.accountservice.service.FeeCalculationService;
import com.stockmarket.accountservice.service.HoldingService;
import com.stockmarket.accountservice.service.LedgerService;
import com.stockmarket.accountservice.service.TaxCalculationService;
import com.stockmarket.accountservice.service.TradeService;
import com.stockmarket.accountservice.service.model.FeeBreakdown;
import com.stockmarket.accountservice.service.model.FeeCharge;
import com.stockmarket.accountservice.service.model.Money;
import com.stockmarket.accountservice.service.model.TaxBreakdown;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Trade execution and recording.
 *
 * A platform trade is one database transaction: the account row is locked, and the trade,
 * ledger entries, fee and tax rows, holding and audit entries commit together or not at
 * all. The Kafka event is sent after commit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final TrackedChargeRecorder trackedChargeRecorder;
    private final AccountAccessService accountAccessService;
    private final MarketDataClient marketDataClient;
    private final FeeCalculationService feeCalculationService;
    private final TaxCalculationService taxCalculationService;
    private final LedgerService ledgerService;
    private final HoldingService holdingService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountMapper mapper;

    @Override
    @Transactional
    public TradeResponse buyTrade(String userId, BuyTradeRequest request) {

        String symbol = normalize(request.getSymbol());
        BigDecimal quantity = BigDecimal.valueOf(request.getQuantity());

        // Price first, before taking the account lock, so the lock is not held during the HTTP call.
        BigDecimal price = marketDataClient.getCurrentPrice(symbol);
        InvestmentAccount account = lockPlatformAccount(request.getAccountId(), userId);

        BigDecimal gross = grossAmount(price, quantity);
        FeeBreakdown fees = feeCalculationService.calculateTotalFees(gross);
        TaxBreakdown taxes = taxCalculationService.calculateTotalTax(TradeSide.BUY, gross, null);
        BigDecimal totalDebit = gross.add(fees.total()).add(taxes.total());

        if (account.getBalance().compareTo(totalDebit) < 0) {
            throw new InsufficientBalanceException(totalDebit, account.getBalance());
        }

        BigDecimal executionPrice = Money.price(price);
        Trade trade = tradeRepository.save(platformTrade(account, symbol, TradeSide.BUY, quantity, executionPrice, gross, fees, taxes)
                .buyPrice(executionPrice)
                .netAmount(totalDebit)
                .build());

        String description = describe(trade);
        ledgerService.postDebit(account, gross, LedgerCategory.TRADE_BUY, ReferenceType.TRADE, trade.getId(), description);
        postCharges(account, trade, fees, taxes);

        Holding holding = holdingService.updateHoldingAfterBuy(account, symbol, quantity, price);

        return complete(account, trade, holding, fees, taxes, AuditAction.BUY, description, request);
    }

    @Override
    @Transactional
    public TradeResponse sellTrade(String userId, SellTradeRequest request) {

        String symbol = normalize(request.getSymbol());
        BigDecimal quantity = BigDecimal.valueOf(request.getQuantity());

        BigDecimal price = marketDataClient.getCurrentPrice(symbol);
        InvestmentAccount account = lockPlatformAccount(request.getAccountId(), userId);

        Holding holding = holdingRepository.findByAccountIdAndSymbol(account.getId(), symbol)
                .filter(h -> h.getQuantity().signum() > 0)
                .orElseThrow(() -> new ValidationException("No " + symbol + " shares held in this account"));

        if (holding.getQuantity().compareTo(quantity) < 0) {
            throw new ValidationException("Insufficient shares of " + symbol + ": held "
                    + holding.getQuantity().stripTrailingZeros().toPlainString() + ", requested " + quantity);
        }

        BigDecimal gross = grossAmount(price, quantity);
        FeeBreakdown fees = feeCalculationService.calculateTotalFees(gross);

        BigDecimal averageCost = holding.getAveragePrice();
        BigDecimal costBasis = Money.amount(averageCost.multiply(quantity));
        BigDecimal realizedProfit = Money.amount(gross.subtract(costBasis).subtract(fees.total()));

        TaxBreakdown taxes = taxCalculationService.calculateTotalTax(TradeSide.SELL, gross, realizedProfit);
        BigDecimal netCredit = gross.subtract(fees.total()).subtract(taxes.total());

        BigDecimal executionPrice = Money.price(price);
        Trade trade = tradeRepository.save(platformTrade(account, symbol, TradeSide.SELL, quantity, executionPrice, gross, fees, taxes)
                .buyPrice(averageCost.signum() > 0 ? averageCost : null)
                .sellPrice(executionPrice)
                .netAmount(netCredit)
                .realizedProfit(realizedProfit)
                .averageCostAtTrade(averageCost)
                .build());

        String description = describe(trade);
        ledgerService.postCredit(account, gross, LedgerCategory.TRADE_SELL, ReferenceType.TRADE, trade.getId(), description);
        // Debits after the credit; if fees and taxes exceed balance + proceeds this throws and rolls back.
        postCharges(account, trade, fees, taxes);

        Holding updated = holdingService.updateHoldingAfterSell(holding, quantity, realizedProfit);

        return complete(account, trade, updated, fees, taxes, AuditAction.SELL,
                description + ", realized " + realizedProfit.toPlainString(), request);
    }

    @Override
    @Transactional
    public TradeResponse createManualTrade(String userId, ManualTradeRequest request) {

        InvestmentAccount account = accountAccessService.getOwnedActiveAccountForUpdate(request.getAccountId(), userId);
        if (!account.isTracking()) {
            throw new ValidationException(account.getBrokerName() + " accounts trade at market price: use /api/v1/trades/buy or /sell");
        }

        BigDecimal buyPrice = request.getBuyPrice();
        BigDecimal sellPrice = request.getSellPrice();
        TradeKind kind = buyPrice != null && sellPrice != null ? TradeKind.CLOSED_POSITION : TradeKind.EXECUTION;
        if (kind == TradeKind.EXECUTION) {
            if (request.getSide() == TradeSide.BUY && buyPrice == null) {
                throw new ValidationException("buyPrice is required for a BUY");
            }
            if (request.getSide() == TradeSide.SELL && sellPrice == null) {
                throw new ValidationException("sellPrice is required for a SELL");
            }
        }

        if (request.getExternalTradeId() != null
                && tradeRepository.existsByAccountIdAndExternalTradeId(account.getId(), request.getExternalTradeId())) {
            throw new DuplicateResourceException("Trade " + request.getExternalTradeId() + " is already recorded", request.getExternalTradeId());
        }

        BigDecimal quantity = request.getQuantity();
        BigDecimal brokerage = Money.amount(zeroIfNull(request.getBrokerage()));
        BigDecimal otherFee = Money.amount(zeroIfNull(request.getFee()));
        BigDecimal tax = Money.amount(zeroIfNull(request.getTax()));
        BigDecimal fees = brokerage.add(otherFee);

        BigDecimal price = kind == TradeKind.CLOSED_POSITION
                ? (request.getSide() == TradeSide.BUY ? sellPrice : buyPrice)   // closing price of the position
                : (request.getSide() == TradeSide.BUY ? buyPrice : sellPrice);
        BigDecimal gross = grossAmount(price, quantity);

        Trade.TradeBuilder builder = Trade.builder()
                .account(account)
                .userId(account.getUserId())
                .symbol(normalize(request.getSymbol()))
                .exchange(request.getExchange())
                .side(request.getSide())
                .tradeKind(kind)
                .source(RecordSource.MANUAL)
                .quantity(quantity)
                .price(price)
                .grossAmount(gross)
                .brokerage(brokerage)
                .otherFee(otherFee)
                .feeAmount(fees)
                .taxAmount(tax)
                .tradeDate(request.getTradeDate())
                .externalTradeId(request.getExternalTradeId())
                .notes(request.getNotes());

        if (kind == TradeKind.CLOSED_POSITION) {
            // Profit of the round trip is (sell - buy) x quantity for long and short positions alike.
            BigDecimal realized = Money.amount(sellPrice.subtract(buyPrice).multiply(quantity).subtract(fees));
            builder.buyPrice(buyPrice).sellPrice(sellPrice).averageCostAtTrade(buyPrice)
                    .realizedProfit(realized).netAmount(realized.subtract(tax));
        } else if (request.getSide() == TradeSide.BUY) {
            builder.buyPrice(buyPrice).netAmount(gross.add(fees).add(tax));
        } else {
            // Realized profit and buy price are filled in by the holdings rebuild below.
            builder.sellPrice(sellPrice).netAmount(gross.subtract(fees).subtract(tax));
        }

        Trade trade = tradeRepository.save(builder.build());
        trackedChargeRecorder.record(account, trade, RecordSource.MANUAL, null);
        List<String> warnings = holdingService.rebuildHoldings(account);

        TradeResponse response = mapper.toTradeResponse(trade);
        auditService.logAccountAction(account.getId(), AuditAction.TRADE_CREATE,
                "Recorded " + describe(trade) + " [trade " + trade.getId() + "]", request, response);
        auditService.logAccountAction(account.getId(), AuditAction.HOLDING_UPDATE,
                "Holdings rebuilt after manual trade " + trade.getId(), null,
                warnings.isEmpty() ? Map.of("symbol", trade.getSymbol()) : Map.of("symbol", trade.getSymbol(), "warnings", warnings));

        log.info("Manual {} {} recorded on account {}", trade.getTradeKind(), describe(trade), account.getAccountNumber());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TradeResponse getTrade(String tradeId, String userId) {

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade", tradeId));
        accountAccessService.getOwnedAccount(trade.getAccount().getId(), userId);
        return mapper.toTradeResponse(trade);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeResponse> getTrades(String accountId, String userId, LocalDate from, LocalDate to) {

        accountAccessService.getOwnedAccount(accountId, userId);

        if (from == null && to == null) {
            return mapper.toTradeResponses(tradeRepository.findByAccountIdOrderByTradeDateDesc(accountId));
        }
        if (from == null || to == null) {
            throw new ValidationException("Give both 'from' and 'to', or neither");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("'from' must not be after 'to'");
        }

        List<Trade> trades = tradeRepository.findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(
                accountId, from.atStartOfDay(), endOfDay(to));
        return mapper.toTradeResponses(trades.reversed());
    }

    private InvestmentAccount lockPlatformAccount(String accountId, String userId) {

        InvestmentAccount account = accountAccessService.getOwnedActiveAccountForUpdate(accountId, userId);
        if (account.isTracking()) {
            throw new ValidationException(account.getBrokerName()
                    + " is a tracking account: record its trades with POST /api/v1/trades or a statement upload");
        }
        return account;
    }

    private static Trade.TradeBuilder platformTrade(InvestmentAccount account, String symbol, TradeSide side,
                                                    BigDecimal quantity, BigDecimal price, BigDecimal gross,
                                                    FeeBreakdown fees, TaxBreakdown taxes) {

        BigDecimal brokerage = fees.charges().stream()
                .filter(charge -> charge.feeMaster().getFeeType() == FeeType.BROKERAGE)
                .map(FeeCharge::amount)
                .reduce(Money.zero(), BigDecimal::add);

        return Trade.builder()
                .account(account)
                .userId(account.getUserId())
                .symbol(symbol)
                .side(side)
                .tradeKind(TradeKind.EXECUTION)
                .source(RecordSource.PLATFORM)
                .quantity(quantity)
                .price(price)
                .grossAmount(gross)
                .brokerage(brokerage)
                .otherFee(fees.total().subtract(brokerage))
                .feeAmount(fees.total())
                .taxAmount(taxes.total())
                .tradeDate(LocalDateTime.now());
    }

    /**
     * Posts the FEE and TAX debits and records the individual fee and tax rows.
     */
    private void postCharges(InvestmentAccount account, Trade trade, FeeBreakdown fees, TaxBreakdown taxes) {

        if (fees.total().signum() > 0) {
            ledgerService.postDebit(account, fees.total(), LedgerCategory.FEE, ReferenceType.TRADE, trade.getId(),
                    "Trade fees for " + trade.getSide() + " " + trade.getSymbol());
            feeCalculationService.recordCharges(account, fees, trade, null);
        }
        if (taxes.total().signum() > 0) {
            ledgerService.postDebit(account, taxes.total(), LedgerCategory.TAX, ReferenceType.TRADE, trade.getId(),
                    "Trade taxes for " + trade.getSide() + " " + trade.getSymbol());
            taxCalculationService.recordCharges(account, taxes, trade, null);
        }
    }

    private TradeResponse complete(InvestmentAccount account, Trade trade, Holding holding, FeeBreakdown fees,
                                   TaxBreakdown taxes, AuditAction action, String details, Object request) {

        TradeResponse response = mapper.toTradeResponse(trade);
        response.setBalanceAfter(account.getBalance());

        String reference = " [trade " + trade.getId() + "]";
        auditService.logAccountAction(account.getId(), action, details + reference, request, response);
        auditService.logAccountAction(account.getId(), AuditAction.FEE_CALCULATION,
                "Fees " + fees.total().toPlainString() + reference, null, mapper.toFeeBreakdownResponse(fees));
        auditService.logAccountAction(account.getId(), AuditAction.TAX_CALCULATION,
                "Taxes " + taxes.total().toPlainString() + reference, null, mapper.toTaxBreakdownResponse(taxes));
        auditService.logAccountAction(account.getId(), AuditAction.HOLDING_UPDATE,
                trade.getSymbol() + " now " + holding.getQuantity().stripTrailingZeros().toPlainString() + reference,
                null, mapper.toHoldingResponse(holding));

        eventPublisher.publishEvent(new TradeExecutedEvent(
                trade.getId(),
                account.getId(),
                account.getUserId(),
                trade.getSymbol(),
                trade.getSide().name(),
                trade.getQuantity().intValueExact(),
                trade.getPrice(),
                trade.getGrossAmount(),
                trade.getFeeAmount(),
                trade.getTaxAmount(),
                trade.getNetAmount(),
                trade.getRealizedProfit(),
                account.getBalance(),
                trade.getTradeDate()));

        log.info("{} executed on account {}: {} -> balance {}",
                trade.getSide(), account.getAccountNumber(), details, account.getBalance());
        return response;
    }

    static BigDecimal grossAmount(BigDecimal price, BigDecimal quantity) {
        return Money.amount(price.multiply(quantity));
    }

    static String normalize(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String describe(Trade trade) {
        return trade.getSide() + " " + trade.getQuantity().stripTrailingZeros().toPlainString() + " " + trade.getSymbol()
                + " @ " + trade.getPrice().stripTrailingZeros().toPlainString();
    }

    private static LocalDateTime endOfDay(LocalDate day) {
        // Microsecond precision matches DATETIME(6); LocalTime.MAX could round up to the next day.
        return day.plusDays(1).atStartOfDay().minusNanos(1_000);
    }
}
