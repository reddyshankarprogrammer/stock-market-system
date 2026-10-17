package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.entity.TaxMaster;
import com.stockmarket.accountservice.enums.CalculationType;
import com.stockmarket.accountservice.enums.TaxType;
import com.stockmarket.accountservice.enums.TradeSide;
import com.stockmarket.accountservice.repository.TaxMasterRepository;
import com.stockmarket.accountservice.service.model.TaxBreakdown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TaxCalculationServiceImplTest {

    @Mock
    private TaxMasterRepository taxMasterRepository;

    private TaxCalculationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaxCalculationServiceImpl(taxMasterRepository, null, null, null);
        lenient().when(taxMasterRepository.findByTaxTypeAndActiveTrue(TaxType.TRADE_TAX))
                .thenReturn(List.of(rule(TaxType.TRADE_TAX, "0.05", TradeSide.SELL)));
        lenient().when(taxMasterRepository.findByTaxTypeAndActiveTrue(TaxType.CAPITAL_GAINS_TAX))
                .thenReturn(List.of(rule(TaxType.CAPITAL_GAINS_TAX, "10", null)));
        lenient().when(taxMasterRepository.findByTaxTypeAndActiveTrue(TaxType.DIVIDEND_TAX))
                .thenReturn(List.of(rule(TaxType.DIVIDEND_TAX, "5", null)));
    }

    @Test
    void tradeTaxOnlyAppliesToItsSide() {
        assertThat(service.calculateTradeTax(TradeSide.BUY, new BigDecimal("1000")).total()).isEqualByComparingTo("0.00");
        assertThat(service.calculateTradeTax(TradeSide.SELL, new BigDecimal("1000")).total()).isEqualByComparingTo("0.50");
    }

    @Test
    void capitalGainsTaxIsChargedOnProfitOnly() {
        assertThat(service.calculateCapitalGainTax(new BigDecimal("78.48")).total()).isEqualByComparingTo("7.85");
        assertThat(service.calculateCapitalGainTax(new BigDecimal("-10")).charges()).isEmpty();
        assertThat(service.calculateCapitalGainTax(BigDecimal.ZERO).charges()).isEmpty();
        assertThat(service.calculateCapitalGainTax(null).charges()).isEmpty();
    }

    @Test
    void dividendTaxIsWithheldFromGross() {
        assertThat(service.calculateDividendTax(new BigDecimal("3.00")).total()).isEqualByComparingTo("0.15");
    }

    @Test
    void totalTaxOnSellCombinesTradeTaxAndCapitalGains() {
        TaxBreakdown breakdown = service.calculateTotalTax(TradeSide.SELL, new BigDecimal("480"), new BigDecimal("78.48"));

        assertThat(breakdown.charges()).hasSize(2);
        assertThat(breakdown.total()).isEqualByComparingTo("8.09");
    }

    @Test
    void totalTaxOnBuyIgnoresRealizedProfit() {
        TaxBreakdown breakdown = service.calculateTotalTax(TradeSide.BUY, new BigDecimal("480"), new BigDecimal("78.48"));

        assertThat(breakdown.charges()).isEmpty();
    }

    @Test
    void fixedTaxIgnoresTheBase() {
        TaxMaster fixed = rule(TaxType.TRADE_TAX, "1.25", null);
        fixed.setCalculationType(CalculationType.FIXED);

        assertThat(TaxCalculationServiceImpl.apply(fixed, new BigDecimal("99999"))).isEqualByComparingTo("1.25");
    }

    private static TaxMaster rule(TaxType type, String rate, TradeSide side) {
        return TaxMaster.builder()
                .code(type + "_TEST")
                .taxType(type)
                .calculationType(CalculationType.PERCENTAGE)
                .rate(new BigDecimal(rate))
                .appliesToSide(side)
                .active(true)
                .build();
    }
}
