package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.entity.AccountCharge;
import com.stockmarket.accountservice.entity.AccountStatement;
import com.stockmarket.accountservice.entity.AccountTax;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.entity.Trade;
import com.stockmarket.accountservice.enums.ChargeType;
import com.stockmarket.accountservice.enums.RecordSource;
import com.stockmarket.accountservice.enums.TaxType;
import com.stockmarket.accountservice.repository.AccountChargeRepository;
import com.stockmarket.accountservice.repository.AccountTaxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the per-trade costs of a manual or imported trade: brokerage and other fees as
 * {@link AccountCharge} rows and the trade tax as an {@link AccountTax} row, all linked to
 * the trade (and statement), so charge and tax summaries include them.
 */
@Component
@RequiredArgsConstructor
public class TrackedChargeRecorder {

    private final AccountChargeRepository chargeRepository;
    private final AccountTaxRepository taxRepository;

    /**
     * @return number of charge rows and tax rows written, as {charges, taxes}
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int[] record(InvestmentAccount account, Trade trade, RecordSource source, AccountStatement statement) {

        List<AccountCharge> charges = new ArrayList<>();
        if (trade.getBrokerage().signum() > 0) {
            charges.add(charge(account, trade, ChargeType.BROKERAGE, trade.getBrokerage(), source, statement, "Brokerage"));
        }
        if (trade.getOtherFee().signum() > 0) {
            charges.add(charge(account, trade, ChargeType.OTHER, trade.getOtherFee(), source, statement, "Trade fees"));
        }
        chargeRepository.saveAll(charges);

        int taxes = 0;
        if (trade.getTaxAmount().signum() > 0) {
            taxRepository.save(AccountTax.builder()
                    .account(account)
                    .trade(trade)
                    .statement(statement)
                    .taxType(TaxType.TRADE_TAX)
                    .source(source)
                    .taxableAmount(trade.getGrossAmount())
                    .taxAmount(trade.getTaxAmount())
                    .chargedAt(trade.getTradeDate())
                    .description("Tax on " + trade.getSide() + " " + trade.getSymbol())
                    .build());
            taxes = 1;
        }
        return new int[]{charges.size(), taxes};
    }

    private static AccountCharge charge(InvestmentAccount account, Trade trade, ChargeType type, BigDecimal amount,
                                        RecordSource source, AccountStatement statement, String description) {
        return AccountCharge.builder()
                .account(account)
                .userId(account.getUserId())
                .trade(trade)
                .statement(statement)
                .chargeType(type)
                .amount(amount)
                .chargeDate(trade.getTradeDate().toLocalDate())
                .source(source)
                .description(description + " " + trade.getSide() + " " + trade.getSymbol())
                .build();
    }
}
