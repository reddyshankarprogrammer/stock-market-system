package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.request.TaxRequest;
import com.stockmarket.accountservice.dto.response.AccountTaxResponse;
import com.stockmarket.accountservice.dto.response.TaxBreakdownResponse;
import com.stockmarket.accountservice.dto.response.MonthAmountResponse;
import com.stockmarket.accountservice.dto.response.TaxMasterResponse;
import com.stockmarket.accountservice.dto.response.TaxSummaryResponse;
import com.stockmarket.accountservice.dto.response.TypeAmountResponse;
import com.stockmarket.accountservice.entity.AccountTax;
import com.stockmarket.accountservice.entity.Dividend;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.entity.TaxMaster;
import com.stockmarket.accountservice.entity.Trade;
import com.stockmarket.accountservice.enums.CalculationType;
import com.stockmarket.accountservice.enums.TaxType;
import com.stockmarket.accountservice.enums.TradeSide;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AccountTaxRepository;
import com.stockmarket.accountservice.repository.TaxMasterRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.TaxCalculationService;
import com.stockmarket.accountservice.service.model.Money;
import com.stockmarket.accountservice.service.model.TaxBreakdown;
import com.stockmarket.accountservice.service.model.TaxCharge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class TaxCalculationServiceImpl implements TaxCalculationService {

    private final TaxMasterRepository taxMasterRepository;
    private final AccountTaxRepository accountTaxRepository;
    private final AccountAccessService accountAccessService;
    private final AccountMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public TaxBreakdown calculateTradeTax(TradeSide side, BigDecimal tradeValue) {
        return breakdown(TaxType.TRADE_TAX, tradeValue,
                rule -> rule.getAppliesToSide() == null || rule.getAppliesToSide() == side);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxBreakdown calculateDividendTax(BigDecimal grossDividend) {
        return breakdown(TaxType.DIVIDEND_TAX, grossDividend, rule -> true);
    }

    /**
     * Only a positive realized profit is taxed; losses are not offset against other trades.
     */
    @Override
    @Transactional(readOnly = true)
    public TaxBreakdown calculateCapitalGainTax(BigDecimal realizedProfit) {
        if (realizedProfit == null || realizedProfit.signum() <= 0) {
            return empty();
        }
        return breakdown(TaxType.CAPITAL_GAINS_TAX, realizedProfit, rule -> true);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxBreakdown calculateTotalTax(TradeSide side, BigDecimal tradeValue, BigDecimal realizedProfit) {

        List<TaxCharge> charges = new ArrayList<>(calculateTradeTax(side, tradeValue).charges());
        if (side == TradeSide.SELL) {
            charges.addAll(calculateCapitalGainTax(realizedProfit).charges());
        }
        return of(charges);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<AccountTax> recordCharges(InvestmentAccount account, TaxBreakdown taxes, Trade trade, Dividend dividend) {

        LocalDateTime now = LocalDateTime.now();
        List<AccountTax> rows = taxes.charges().stream()
                .map(charge -> AccountTax.builder()
                        .account(account)
                        .taxMaster(charge.taxMaster())
                        .trade(trade)
                        .dividend(dividend)
                        .taxType(charge.taxMaster().getTaxType())
                        .taxableAmount(charge.taxableAmount())
                        .taxAmount(charge.amount())
                        .chargedAt(now)
                        .build())
                .toList();

        return accountTaxRepository.saveAll(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxBreakdownResponse previewTax(TaxRequest request) {
        return mapper.toTaxBreakdownResponse(
                calculateTotalTax(request.getSide(), request.getTradeValue(), request.getRealizedProfit()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxMasterResponse> getTaxRules() {
        return mapper.toTaxMasterResponses(taxMasterRepository.findByActiveTrueOrderByTaxType());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountTaxResponse> getAccountTaxes(String accountId, String userId) {

        accountAccessService.getOwnedAccount(accountId, userId);
        return mapper.toAccountTaxResponses(accountTaxRepository.findByAccountIdOrderByChargedAtDesc(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public TaxSummaryResponse getYearlyTaxSummary(String accountId, String userId, int year) {

        accountAccessService.getOwnedAccount(accountId, userId);
        LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime to = from.plusYears(1);

        List<TypeAmountResponse> byType = accountTaxRepository.sumByTypeForPeriod(accountId, from, to).stream()
                .map(row -> new TypeAmountResponse(row.getType().name(), Money.amount(row.getTotal())))
                .sorted(Comparator.comparing(TypeAmountResponse::getAmount).reversed())
                .toList();
        List<MonthAmountResponse> byMonth = accountTaxRepository.sumByMonthForPeriod(accountId, from, to).stream()
                .map(row -> new MonthAmountResponse(row.getMonth(), Money.amount(row.getTotal())))
                .toList();

        return TaxSummaryResponse.builder()
                .accountId(accountId)
                .year(year)
                .byType(byType)
                .byMonth(byMonth)
                .totalTaxes(byType.stream().map(TypeAmountResponse::getAmount).reduce(Money.zero(), BigDecimal::add))
                .build();
    }

    /**
     * FIXED -> rate; PERCENTAGE -> rate% of the taxable amount; 2 decimals.
     */
    static BigDecimal apply(TaxMaster rule, BigDecimal taxableAmount) {
        return rule.getCalculationType() == CalculationType.FIXED
                ? Money.amount(rule.getRate())
                : Money.percentOf(taxableAmount, rule.getRate());
    }

    private TaxBreakdown breakdown(TaxType type, BigDecimal taxableAmount, Predicate<TaxMaster> applies) {

        BigDecimal base = Money.amount(taxableAmount);
        List<TaxCharge> charges = taxMasterRepository.findByTaxTypeAndActiveTrue(type).stream()
                .filter(applies)
                .map(rule -> new TaxCharge(rule, base, apply(rule, base)))
                .filter(charge -> charge.amount().signum() > 0)
                .toList();

        return of(charges);
    }

    private static TaxBreakdown of(List<TaxCharge> charges) {
        BigDecimal total = charges.stream()
                .map(TaxCharge::amount)
                .reduce(Money.zero(), BigDecimal::add);
        return new TaxBreakdown(List.copyOf(charges), total);
    }

    private static TaxBreakdown empty() {
        return new TaxBreakdown(List.of(), Money.zero());
    }
}
