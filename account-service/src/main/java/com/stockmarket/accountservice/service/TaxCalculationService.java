package com.stockmarket.accountservice.service;

import com.stockmarket.accountservice.dto.request.TaxRequest;
import com.stockmarket.accountservice.dto.response.AccountTaxResponse;
import com.stockmarket.accountservice.dto.response.TaxBreakdownResponse;
import com.stockmarket.accountservice.dto.response.TaxMasterResponse;
import com.stockmarket.accountservice.dto.response.TaxSummaryResponse;
import com.stockmarket.accountservice.entity.AccountTax;
import com.stockmarket.accountservice.entity.Dividend;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.entity.Trade;
import com.stockmarket.accountservice.enums.TradeSide;
import com.stockmarket.accountservice.service.model.TaxBreakdown;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tax engine driven by the active {@code tax_master} rows.
 */
public interface TaxCalculationService {

    /**
     * Transaction tax on the trade value; rules may be limited to BUY or SELL.
     */
    TaxBreakdown calculateTradeTax(TradeSide side, BigDecimal tradeValue);

    /**
     * Withholding tax on a gross dividend.
     */
    TaxBreakdown calculateDividendTax(BigDecimal grossDividend);

    /**
     * Capital gains tax on realized profit; zero for a loss or break-even.
     */
    TaxBreakdown calculateCapitalGainTax(BigDecimal realizedProfit);

    /**
     * Trade tax plus, for a SELL with a profit, capital gains tax.
     */
    TaxBreakdown calculateTotalTax(TradeSide side, BigDecimal tradeValue, BigDecimal realizedProfit);

    /**
     * Persists one {@code AccountTax} per charge, linked to the trade or dividend.
     */
    List<AccountTax> recordCharges(InvestmentAccount account, TaxBreakdown taxes, Trade trade, Dividend dividend);

    TaxBreakdownResponse previewTax(TaxRequest request);

    List<TaxMasterResponse> getTaxRules();

    List<AccountTaxResponse> getAccountTaxes(String accountId, String userId);

    /**
     * Taxes of all sources (engine, manual, imported) for a calendar year, per type and per month.
     */
    TaxSummaryResponse getYearlyTaxSummary(String accountId, String userId, int year);
}
