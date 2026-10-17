package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.response.AccountChargeResponse;
import com.stockmarket.accountservice.dto.response.ChargeSummaryResponse;
import com.stockmarket.accountservice.dto.response.TypeAmountResponse;
import com.stockmarket.accountservice.enums.ChargeType;
import com.stockmarket.accountservice.enums.FeeType;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AccountChargeRepository;
import com.stockmarket.accountservice.repository.AccountFeeRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.ChargeService;
import com.stockmarket.accountservice.service.model.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService {

    private final AccountChargeRepository chargeRepository;
    private final AccountFeeRepository feeRepository;
    private final AccountAccessService accountAccessService;
    private final AccountMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<AccountChargeResponse> getCharges(String accountId, String userId) {

        accountAccessService.getOwnedAccount(accountId, userId);
        return mapper.toAccountChargeResponses(chargeRepository.findByAccountIdOrderByChargeDateDesc(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public ChargeSummaryResponse getChargeSummary(String accountId, String userId, int year) {

        accountAccessService.getOwnedAccount(accountId, userId);
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        Map<ChargeType, BigDecimal> totals = new EnumMap<>(ChargeType.class);
        chargeRepository.sumByTypeForPeriod(accountId, from, to)
                .forEach(row -> totals.merge(row.getType(), row.getTotal(), BigDecimal::add));
        feeRepository.sumByTypeForPeriod(accountId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .forEach(row -> totals.merge(toChargeType(row.getType()), row.getTotal(), BigDecimal::add));

        List<TypeAmountResponse> byType = totals.entrySet().stream()
                .map(entry -> new TypeAmountResponse(entry.getKey().name(), Money.amount(entry.getValue())))
                .sorted(Comparator.comparing(TypeAmountResponse::getAmount).reversed())
                .toList();

        return ChargeSummaryResponse.builder()
                .accountId(accountId)
                .year(year)
                .byType(byType)
                .totalCharges(byType.stream().map(TypeAmountResponse::getAmount).reduce(Money.zero(), BigDecimal::add))
                .build();
    }

    /**
     * Maps a platform fee-engine fee onto the charge types used for imported charges.
     */
    static ChargeType toChargeType(FeeType feeType) {
        return switch (feeType) {
            case BROKERAGE -> ChargeType.BROKERAGE;
            case EXCHANGE -> ChargeType.EXCHANGE_FEE;
            case PLATFORM, WITHDRAWAL -> ChargeType.OTHER;
        };
    }
}
