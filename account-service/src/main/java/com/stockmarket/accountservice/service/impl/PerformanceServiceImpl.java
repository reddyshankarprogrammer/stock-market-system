package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.response.HoldingResponse;
import com.stockmarket.accountservice.dto.response.PerformanceResponse;
import com.stockmarket.accountservice.entity.AccountPerformance;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AccountChargeRepository;
import com.stockmarket.accountservice.repository.AccountFeeRepository;
import com.stockmarket.accountservice.repository.AccountPerformanceRepository;
import com.stockmarket.accountservice.repository.AccountTaxRepository;
import com.stockmarket.accountservice.repository.TradeRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.HoldingService;
import com.stockmarket.accountservice.service.PerformanceService;
import com.stockmarket.accountservice.service.model.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private final AccountAccessService accountAccessService;
    private final HoldingService holdingService;
    private final TradeRepository tradeRepository;
    private final AccountFeeRepository feeRepository;
    private final AccountChargeRepository chargeRepository;
    private final AccountTaxRepository taxRepository;
    private final AccountPerformanceRepository performanceRepository;
    private final AccountMapper mapper;

    @Override
    @Transactional
    public PerformanceResponse calculatePerformance(String accountId, String userId) {

        InvestmentAccount account = accountAccessService.getOwnedAccount(accountId, userId);
        List<HoldingResponse> holdings = holdingService.valueOpenPositions(accountId);

        BigDecimal realized = Money.amount(tradeRepository.sumRealizedProfitByAccountId(accountId));
        BigDecimal unrealized = holdings.stream()
                .map(h -> h.getUnrealizedProfit() != null ? h.getUnrealizedProfit() : BigDecimal.ZERO)
                .reduce(Money.zero(), BigDecimal::add);
        BigDecimal feesInsideRealized = Money.amount(tradeRepository.sumFeesDeductedFromRealizedProfit(accountId));
        BigDecimal gross = Money.amount(realized.add(unrealized).add(feesInsideRealized));

        BigDecimal charges = Money.amount(feeRepository.sumFeesByAccountId(accountId)
                .add(chargeRepository.sumByAccountId(accountId)));
        BigDecimal taxes = Money.amount(taxRepository.sumTaxesByAccountId(accountId));
        BigDecimal net = Money.amount(gross.subtract(charges).subtract(taxes));
        boolean pricesLive = holdings.stream().allMatch(h -> h.getCurrentPrice() != null);

        AccountPerformance performance = performanceRepository.findByAccountId(accountId)
                .orElseGet(() -> AccountPerformance.builder().account(account).userId(account.getUserId()).build());
        performance.setGrossProfit(gross);
        performance.setRealizedProfit(realized);
        performance.setUnrealizedProfit(unrealized);
        performance.setTotalCharges(charges);
        performance.setTotalTaxes(taxes);
        performance.setNetProfit(net);
        performance.setPricesLive(pricesLive);
        performance.setCalculatedAt(LocalDateTime.now());

        AccountPerformance saved = performanceRepository.save(performance);
        log.debug("Performance of account {}: gross {} net {}", accountId, gross, net);
        return mapper.toPerformanceResponse(saved);
    }
}
