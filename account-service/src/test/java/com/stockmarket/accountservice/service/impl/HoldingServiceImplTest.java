package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.entity.Holding;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.entity.Trade;
import com.stockmarket.accountservice.enums.BrokerName;
import com.stockmarket.accountservice.enums.RecordSource;
import com.stockmarket.accountservice.enums.TradeKind;
import com.stockmarket.accountservice.enums.TradeSide;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.repository.HoldingRepository;
import com.stockmarket.accountservice.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingServiceImplTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private TradeRepository tradeRepository;

    private HoldingServiceImpl service;
    private InvestmentAccount account;

    @BeforeEach
    void setUp() {
        service = new HoldingServiceImpl(holdingRepository, null, null, null, tradeRepository);
        account = new InvestmentAccount();
        account.setId("acc-1");
        account.setUserId("user-1");
        account.setBrokerName(BrokerName.ZERODHA);
        lenient().when(holdingRepository.save(any(Holding.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void averagePriceIsWeightedByQuantity() {
        // (10 x 100 + 30 x 120) / 40 = 115
        assertThat(service.calculateAveragePrice(bd("10"), bd("100"), bd("30"), bd("120")))
                .isEqualByComparingTo("115");
    }

    @Test
    void averagePriceSupportsFractionalUnits() {
        // (0.5 x 60000 + 0.25 x 72000) / 0.75 = 64000
        assertThat(service.calculateAveragePrice(bd("0.5"), bd("60000"), bd("0.25"), bd("72000")))
                .isEqualByComparingTo("64000");
    }

    @Test
    void averagePriceOfFirstPurchaseIsThePrice() {
        assertThat(service.calculateAveragePrice(BigDecimal.ZERO, BigDecimal.ZERO, bd("7"), bd("33.3333")))
                .isEqualByComparingTo("33.3333");
    }

    @Test
    void averagePriceRejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> service.calculateAveragePrice(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buyCreatesThenExtendsTheHolding() {
        when(holdingRepository.findByAccountIdAndSymbol("acc-1", "AAPL")).thenReturn(Optional.empty());

        Holding first = service.updateHoldingAfterBuy(account, "AAPL", bd("10"), bd("100"));
        assertThat(first.getQuantity()).isEqualByComparingTo("10");
        assertThat(first.getUserId()).isEqualTo("user-1");
        assertThat(first.getTotalCost()).isEqualByComparingTo("1000.00");

        when(holdingRepository.findByAccountIdAndSymbol("acc-1", "AAPL")).thenReturn(Optional.of(first));
        Holding second = service.updateHoldingAfterBuy(account, "AAPL", bd("30"), bd("120"));

        assertThat(second.getQuantity()).isEqualByComparingTo("40");
        assertThat(second.getAveragePrice()).isEqualByComparingTo("115");
        assertThat(second.getTotalCost()).isEqualByComparingTo("4600.00");
    }

    @Test
    void sellKeepsAveragePriceAndAccumulatesRealizedProfit() {
        Holding holding = holding("AAPL", "10", "100");

        service.updateHoldingAfterSell(holding, bd("4"), bd("78.48"));
        service.updateHoldingAfterSell(holding, bd("6"), bd("-12.00"));

        assertThat(holding.getQuantity()).isZero();
        assertThat(holding.getAveragePrice()).isEqualByComparingTo("100");
        assertThat(holding.getTotalCost()).isEqualByComparingTo("0.00");
        assertThat(holding.getRealizedProfit()).isEqualByComparingTo("66.48");
    }

    @Test
    void sellingMoreThanHeldIsRejected() {
        assertThatThrownBy(() -> service.updateHoldingAfterSell(holding("AAPL", "3", "10"), bd("4"), BigDecimal.ZERO))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rebuildReplaysTradesInOrderAndComputesRealizedProfit() {
        Trade buy1 = trade(TradeKind.EXECUTION, TradeSide.BUY, "INFY", "10", "1500", "0", 1);
        Trade buy2 = trade(TradeKind.EXECUTION, TradeSide.BUY, "INFY", "10", "1700", "0", 2);
        Trade sell = trade(TradeKind.EXECUTION, TradeSide.SELL, "INFY", "5", "1800", "20", 3);
        Trade closed = trade(TradeKind.CLOSED_POSITION, TradeSide.BUY, "TCS", "2", "3600", "0", 4);
        closed.setRealizedProfit(bd("150.00"));
        when(holdingRepository.findByAccountId("acc-1")).thenReturn(new ArrayList<>());
        when(tradeRepository.findAllForReplay("acc-1")).thenReturn(List.of(buy1, buy2, sell, closed));

        List<String> warnings = service.rebuildHoldings(account);

        Map<String, Holding> saved = savedHoldings();
        Holding infy = saved.get("INFY");
        assertThat(warnings).isEmpty();
        assertThat(infy.getQuantity()).isEqualByComparingTo("15");
        assertThat(infy.getAveragePrice()).isEqualByComparingTo("1600");
        // (1800 - 1600) x 5 - 20 fees = 980
        assertThat(sell.getRealizedProfit()).isEqualByComparingTo("980.00");
        assertThat(infy.getRealizedProfit()).isEqualByComparingTo("980.00");
        // closed positions add realized profit but never units
        assertThat(saved.get("TCS").getQuantity()).isZero();
        assertThat(saved.get("TCS").getRealizedProfit()).isEqualByComparingTo("150.00");
    }

    @Test
    void rebuildWarnsWhenSellingMoreThanKnown() {
        Trade buy = trade(TradeKind.EXECUTION, TradeSide.BUY, "SBIN", "5", "600", "0", 1);
        Trade sell = trade(TradeKind.EXECUTION, TradeSide.SELL, "SBIN", "8", "650", "0", 2);
        when(holdingRepository.findByAccountId("acc-1")).thenReturn(new ArrayList<>());
        when(tradeRepository.findAllForReplay("acc-1")).thenReturn(List.of(buy, sell));

        List<String> warnings = service.rebuildHoldings(account);

        assertThat(warnings).singleElement().asString().contains("exceeds");
        assertThat(savedHoldings().get("SBIN").getQuantity()).isZero();
        // realized on the 5 known units only: (650 - 600) x 5
        assertThat(sell.getRealizedProfit()).isEqualByComparingTo("250.00");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Holding> savedHoldings() {
        ArgumentCaptor<Iterable<Holding>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(holdingRepository).saveAll(captor.capture());
        return StreamSupport.stream(captor.getValue().spliterator(), false)
                .collect(Collectors.toMap(Holding::getSymbol, Function.identity()));
    }

    private Trade trade(TradeKind kind, TradeSide side, String symbol, String qty, String price, String fee, int minute) {
        return Trade.builder()
                .account(account)
                .userId("user-1")
                .symbol(symbol)
                .side(side)
                .tradeKind(kind)
                .source(RecordSource.IMPORT)
                .quantity(bd(qty))
                .price(bd(price))
                .grossAmount(bd(price).multiply(bd(qty)))
                .feeAmount(bd(fee))
                .taxAmount(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .tradeDate(LocalDateTime.of(2026, 10, 1, 10, minute))
                .build();
    }

    private Holding holding(String symbol, String quantity, String average) {
        return Holding.builder()
                .account(account)
                .userId("user-1")
                .symbol(symbol)
                .quantity(bd(quantity))
                .averagePrice(bd(average))
                .totalCost(bd(average).multiply(bd(quantity)))
                .realizedProfit(BigDecimal.ZERO)
                .build();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
