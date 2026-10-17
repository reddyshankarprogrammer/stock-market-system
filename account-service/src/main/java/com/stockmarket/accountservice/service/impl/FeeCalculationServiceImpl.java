package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.request.FeeRequest;
import com.stockmarket.accountservice.dto.response.AccountFeeResponse;
import com.stockmarket.accountservice.dto.response.FeeBreakdownResponse;
import com.stockmarket.accountservice.dto.response.FeeMasterResponse;
import com.stockmarket.accountservice.entity.AccountFee;
import com.stockmarket.accountservice.entity.AccountFundTransaction;
import com.stockmarket.accountservice.entity.FeeMaster;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.entity.Trade;
import com.stockmarket.accountservice.enums.CalculationType;
import com.stockmarket.accountservice.enums.FeeCalculationKind;
import com.stockmarket.accountservice.enums.FeeType;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AccountFeeRepository;
import com.stockmarket.accountservice.repository.FeeMasterRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.FeeCalculationService;
import com.stockmarket.accountservice.service.model.FeeBreakdown;
import com.stockmarket.accountservice.service.model.FeeCharge;
import com.stockmarket.accountservice.service.model.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeeCalculationServiceImpl implements FeeCalculationService {

    /**
     * Fees charged on every trade, in breakdown order.
     */
    private static final List<FeeType> TRADE_FEES = List.of(FeeType.BROKERAGE, FeeType.PLATFORM, FeeType.EXCHANGE);

    private final FeeMasterRepository feeMasterRepository;
    private final AccountFeeRepository accountFeeRepository;
    private final AccountAccessService accountAccessService;
    private final AccountMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateBrokerage(BigDecimal tradeValue) {
        return amountOf(FeeType.BROKERAGE, tradeValue);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculatePlatformFee(BigDecimal tradeValue) {
        return amountOf(FeeType.PLATFORM, tradeValue);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateExchangeFee(BigDecimal tradeValue) {
        return amountOf(FeeType.EXCHANGE, tradeValue);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateWithdrawalFee(BigDecimal amount) {
        return amountOf(FeeType.WITHDRAWAL, amount);
    }

    @Override
    @Transactional(readOnly = true)
    public FeeBreakdown calculateTotalFees(BigDecimal tradeValue) {
        return breakdown(TRADE_FEES, tradeValue);
    }

    @Override
    @Transactional(readOnly = true)
    public FeeBreakdown calculateWithdrawalFees(BigDecimal amount) {
        return breakdown(List.of(FeeType.WITHDRAWAL), amount);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<AccountFee> recordCharges(InvestmentAccount account, FeeBreakdown fees,
                                          Trade trade, AccountFundTransaction fundTransaction) {

        LocalDateTime now = LocalDateTime.now();
        List<AccountFee> rows = fees.charges().stream()
                .map(charge -> AccountFee.builder()
                        .account(account)
                        .feeMaster(charge.feeMaster())
                        .trade(trade)
                        .fundTransaction(fundTransaction)
                        .feeType(charge.feeMaster().getFeeType())
                        .baseAmount(charge.baseAmount())
                        .feeAmount(charge.amount())
                        .chargedAt(now)
                        .build())
                .toList();

        return accountFeeRepository.saveAll(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public FeeBreakdownResponse previewFees(FeeRequest request) {

        FeeBreakdown breakdown = request.getKind() == FeeCalculationKind.WITHDRAWAL
                ? calculateWithdrawalFees(request.getAmount())
                : calculateTotalFees(request.getAmount());

        return mapper.toFeeBreakdownResponse(breakdown);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeMasterResponse> getFeeSchedule() {
        return mapper.toFeeMasterResponses(feeMasterRepository.findByActiveTrueOrderByFeeType());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountFeeResponse> getAccountFees(String accountId, String userId) {

        accountAccessService.getOwnedAccount(accountId, userId);
        return mapper.toAccountFeeResponses(accountFeeRepository.findByAccountIdOrderByChargedAtDesc(accountId));
    }

    /**
     * Applies a schedule entry to a base amount:
     * FIXED -> value; PERCENTAGE -> value% of base; then min/max clamp; 2 decimals.
     */
    static BigDecimal apply(FeeMaster feeMaster, BigDecimal baseAmount) {

        BigDecimal fee = feeMaster.getCalculationType() == CalculationType.FIXED
                ? feeMaster.getValue()
                : Money.percentOf(baseAmount, feeMaster.getValue());

        if (feeMaster.getMinAmount() != null && fee.compareTo(feeMaster.getMinAmount()) < 0) {
            fee = feeMaster.getMinAmount();
        }
        if (feeMaster.getMaxAmount() != null && fee.compareTo(feeMaster.getMaxAmount()) > 0) {
            fee = feeMaster.getMaxAmount();
        }
        return Money.amount(fee);
    }

    private BigDecimal amountOf(FeeType type, BigDecimal baseAmount) {
        return charge(type, baseAmount).map(FeeCharge::amount).orElse(Money.zero());
    }

    private Optional<FeeCharge> charge(FeeType type, BigDecimal baseAmount) {
        BigDecimal base = Money.amount(baseAmount);
        return feeMasterRepository.findFirstByFeeTypeAndActiveTrue(type)
                .map(master -> new FeeCharge(master, base, apply(master, base)));
    }

    private FeeBreakdown breakdown(List<FeeType> types, BigDecimal baseAmount) {

        List<FeeCharge> charges = types.stream()
                .map(type -> charge(type, baseAmount))
                .flatMap(Optional::stream)
                .filter(charge -> charge.amount().signum() > 0)
                .toList();

        BigDecimal total = charges.stream()
                .map(FeeCharge::amount)
                .reduce(Money.zero(), BigDecimal::add);

        return new FeeBreakdown(Money.amount(baseAmount), charges, total);
    }
}
