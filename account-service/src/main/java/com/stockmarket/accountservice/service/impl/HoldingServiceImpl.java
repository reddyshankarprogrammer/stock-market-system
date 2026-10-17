package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.client.MarketDataClient;
import com.stockmarket.accountservice.dto.response.HoldingResponse;
import com.stockmarket.accountservice.entity.Holding;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.entity.Trade;
import com.stockmarket.accountservice.enums.RecordSource;
import com.stockmarket.accountservice.enums.TradeKind;
import com.stockmarket.accountservice.enums.TradeSide;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.HoldingRepository;
import com.stockmarket.accountservice.repository.TradeRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.HoldingService;
import com.stockmarket.accountservice.service.model.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldingServiceImpl implements HoldingService {

    private final HoldingRepository holdingRepository;
    private final AccountAccessService accountAccessService;
    private final MarketDataClient marketDataClient;
    private final AccountMapper mapper;
    private final TradeRepository tradeRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Holding updateHoldingAfterBuy(InvestmentAccount account, String symbol,
                                         BigDecimal quantity, BigDecimal price) {

        Holding holding = holdingRepository.findByAccountIdAndSymbol(account.getId(), symbol)
                .orElseGet(() -> emptyHolding(account, symbol));

        applyBuy(holding, quantity, price);

        log.debug("Holding {} on account {}: qty {} avg {}",
                symbol, account.getId(), holding.getQuantity(), holding.getAveragePrice());
        return holdingRepository.save(holding);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Holding updateHoldingAfterSell(Holding holding, BigDecimal quantity, BigDecimal realizedProfit) {

        if (quantity.signum() <= 0 || quantity.compareTo(holding.getQuantity()) > 0) {
            throw new ValidationException("Cannot sell " + quantity.toPlainString() + " " + holding.getSymbol()
                    + ": " + holding.getQuantity().stripTrailingZeros().toPlainString() + " held");
        }

        applySell(holding, quantity, realizedProfit);
        return holdingRepository.save(holding);
    }

    @Override
    public BigDecimal calculateAveragePrice(BigDecimal currentQuantity, BigDecimal currentAverage,
                                            BigDecimal addQuantity, BigDecimal addPrice) {

        if (addQuantity == null || addQuantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive: " + addQuantity);
        }
        if (currentQuantity.signum() < 0) {
            throw new IllegalArgumentException("Current quantity must not be negative: " + currentQuantity);
        }

        BigDecimal currentCost = currentAverage.multiply(currentQuantity);
        BigDecimal addedCost = addPrice.multiply(addQuantity);
        BigDecimal totalQuantity = currentQuantity.add(addQuantity);

        return currentCost.add(addedCost).divide(totalQuantity, Money.QUANTITY_SCALE, Money.ROUNDING);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<String> rebuildHoldings(InvestmentAccount account) {

        List<String> warnings = new ArrayList<>();
        Map<String, Holding> bySymbol = new LinkedHashMap<>();

        // Start from the stored rows (reset), so symbols that no longer have trades end at zero.
        for (Holding existing : holdingRepository.findByAccountId(account.getId())) {
            reset(existing);
            bySymbol.put(existing.getSymbol(), existing);
        }

        for (Trade trade : tradeRepository.findAllForReplay(account.getId())) {
            Holding holding = bySymbol.computeIfAbsent(trade.getSymbol(), symbol -> emptyHolding(account, symbol));

            if (trade.getTradeKind() == TradeKind.CLOSED_POSITION) {
                // A closed position never changes the units held; it only adds realized P&L.
                holding.setRealizedProfit(Money.amount(holding.getRealizedProfit().add(nullToZero(trade.getRealizedProfit()))));
            } else if (trade.getSide() == TradeSide.BUY) {
                applyBuy(holding, trade.getQuantity(), trade.getPrice());
            } else {
                replaySell(holding, trade, warnings);
            }
        }

        holdingRepository.saveAll(bySymbol.values());
        log.info("Rebuilt {} holdings for account {} ({} warnings)", bySymbol.size(), account.getId(), warnings.size());
        return warnings;
    }

    @Override
    @Transactional
    public List<HoldingResponse> getHoldings(String accountId, String userId) {

        accountAccessService.getOwnedAccount(accountId, userId);
        return holdingRepository.findOpenPositions(accountId).stream()
                .map(holding -> toValuedResponse(holding, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoldingResponse> valueOpenPositions(String accountId) {
        return holdingRepository.findOpenPositions(accountId).stream()
                .map(holding -> toValuedResponse(holding, false))
                .toList();
    }

    /**
     * SELL during replay: realized profit is computed from the average cost at that point.
     * PLATFORM trades keep the realized profit they were executed with.
     */
    private void replaySell(Holding holding, Trade trade, List<String> warnings) {

        BigDecimal sellable = trade.getQuantity().min(holding.getQuantity());
        if (sellable.compareTo(trade.getQuantity()) < 0) {
            warnings.add("SELL of " + trade.getQuantity().stripTrailingZeros().toPlainString() + " " + trade.getSymbol()
                    + " on " + trade.getTradeDate().toLocalDate() + " exceeds the "
                    + holding.getQuantity().stripTrailingZeros().toPlainString()
                    + " units known to be held (earlier buys may be missing); realized profit covers the known units only");
        }

        BigDecimal realized = trade.getRealizedProfit();
        if (trade.getSource() != RecordSource.PLATFORM) {
            BigDecimal gross = trade.getPrice().subtract(holding.getAveragePrice()).multiply(sellable);
            realized = Money.amount(gross.subtract(trade.getFeeAmount()));
            trade.setRealizedProfit(realized);
            trade.setAverageCostAtTrade(holding.getAveragePrice());
            trade.setBuyPrice(holding.getAveragePrice().signum() > 0 ? holding.getAveragePrice() : null);
        }

        if (sellable.signum() > 0) {
            applySell(holding, sellable, nullToZero(realized));
        } else {
            holding.setRealizedProfit(Money.amount(holding.getRealizedProfit().add(nullToZero(realized))));
        }
    }

    private void applyBuy(Holding holding, BigDecimal quantity, BigDecimal price) {

        BigDecimal newAverage = calculateAveragePrice(holding.getQuantity(), holding.getAveragePrice(), quantity, price);

        holding.setQuantity(holding.getQuantity().add(quantity));
        holding.setAveragePrice(newAverage);
        holding.setTotalCost(Money.amount(holding.getTotalCost().add(price.multiply(quantity))));
    }

    private void applySell(Holding holding, BigDecimal quantity, BigDecimal realizedProfit) {

        BigDecimal remaining = holding.getQuantity().subtract(quantity);

        holding.setQuantity(remaining);
        holding.setTotalCost(Money.amount(holding.getAveragePrice().multiply(remaining)));
        holding.setRealizedProfit(Money.amount(holding.getRealizedProfit().add(realizedProfit)));
        if (remaining.signum() == 0) {
            holding.setMarketValue(null);
            holding.setUnrealizedPnl(null);
        }
    }

    /**
     * Adds live valuation, falling back to the stored snapshot (last known price).
     * When {@code persist} is true, a fresh price is written back to the holding.
     */
    private HoldingResponse toValuedResponse(Holding holding, boolean persist) {

        Optional<BigDecimal> livePrice = marketDataClient.findCurrentPrice(holding.getSymbol());

        if (livePrice.isPresent()) {
            BigDecimal price = livePrice.get();
            holding.setCurrentPrice(price);
            holding.setMarketValue(Money.amount(price.multiply(holding.getQuantity())));
            holding.setUnrealizedPnl(Money.amount(holding.getMarketValue().subtract(holding.getTotalCost())));
            holding.setPriceUpdatedAt(LocalDateTime.now());
            if (persist) {
                holdingRepository.save(holding);
            }
        }

        HoldingResponse response = mapper.toHoldingResponse(holding);
        if (response.getUnrealizedProfit() != null) {
            response.setUnrealizedProfitPercent(Money.percentage(response.getUnrealizedProfit(), holding.getTotalCost()));
        }
        return response;
    }

    private static Holding emptyHolding(InvestmentAccount account, String symbol) {
        return Holding.builder()
                .account(account)
                .userId(account.getUserId())
                .symbol(symbol)
                .quantity(BigDecimal.ZERO)
                .averagePrice(BigDecimal.ZERO)
                .totalCost(Money.zero())
                .realizedProfit(Money.zero())
                .build();
    }

    private static void reset(Holding holding) {
        holding.setQuantity(BigDecimal.ZERO);
        holding.setAveragePrice(BigDecimal.ZERO);
        holding.setTotalCost(Money.zero());
        holding.setRealizedProfit(Money.zero());
        holding.setMarketValue(null);
        holding.setUnrealizedPnl(null);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
