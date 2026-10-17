package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.request.DividendRequest;
import com.stockmarket.accountservice.dto.response.DividendResponse;
import com.stockmarket.accountservice.entity.Dividend;
import com.stockmarket.accountservice.entity.Holding;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.enums.AuditAction;
import com.stockmarket.accountservice.enums.LedgerCategory;
import com.stockmarket.accountservice.enums.ReferenceType;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.DividendRepository;
import com.stockmarket.accountservice.repository.HoldingRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.AuditService;
import com.stockmarket.accountservice.service.DividendService;
import com.stockmarket.accountservice.service.LedgerService;
import com.stockmarket.accountservice.service.TaxCalculationService;
import com.stockmarket.accountservice.service.model.Money;
import com.stockmarket.accountservice.service.model.TaxBreakdown;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class DividendServiceImpl implements DividendService {

    private final DividendRepository dividendRepository;
    private final HoldingRepository holdingRepository;
    private final AccountAccessService accountAccessService;
    private final TaxCalculationService taxCalculationService;
    private final LedgerService ledgerService;
    private final AuditService auditService;
    private final AccountMapper mapper;

    /**
     * Credits the gross dividend and debits the withholding tax as two ledger entries,
     * so the statement shows both.
     */
    @Override
    @Transactional
    public DividendResponse postDividend(String accountId, DividendRequest request) {

        InvestmentAccount account = accountAccessService.getActiveAccountForUpdate(accountId);
        String symbol = request.getSymbol().trim().toUpperCase(Locale.ROOT);

        Holding holding = holdingRepository.findByAccountIdAndSymbol(accountId, symbol)
                .filter(h -> h.getQuantity().signum() > 0)
                .orElseThrow(() -> new ValidationException("No " + symbol + " shares held; no dividend due"));

        BigDecimal gross = Money.amount(request.getDividendPerShare().multiply(holding.getQuantity()));
        if (gross.signum() <= 0) {
            throw new ValidationException("Dividend amount rounds to zero");
        }

        TaxBreakdown taxes = taxCalculationService.calculateDividendTax(gross);
        BigDecimal net = gross.subtract(taxes.total());

        Dividend dividend = dividendRepository.save(Dividend.builder()
                .account(account)
                .symbol(symbol)
                .quantity(holding.getQuantity())
                .dividendPerShare(request.getDividendPerShare())
                .grossAmount(gross)
                .taxAmount(taxes.total())
                .netAmount(net)
                .paymentDate(request.getPaymentDate())
                .build());

        ledgerService.postCredit(account, gross, LedgerCategory.DIVIDEND, ReferenceType.DIVIDEND, dividend.getId(),
                "Dividend " + symbol + " " + holding.getQuantity().stripTrailingZeros().toPlainString() + " x " + request.getDividendPerShare().toPlainString());

        if (taxes.total().signum() > 0) {
            ledgerService.postDebit(account, taxes.total(), LedgerCategory.TAX, ReferenceType.DIVIDEND, dividend.getId(),
                    "Dividend withholding tax " + symbol);
            taxCalculationService.recordCharges(account, taxes, null, dividend);
        }

        auditService.logAccountAction(accountId, AuditAction.DIVIDEND,
                "Dividend " + symbol + " gross " + gross.toPlainString() + ", net " + net.toPlainString());
        log.info("Dividend {} on account {}: net {}", symbol, account.getAccountNumber(), net);

        return mapper.toDividendResponse(dividend);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DividendResponse> getDividends(String accountId, String userId) {

        accountAccessService.getOwnedAccount(accountId, userId);
        return mapper.toDividendResponses(dividendRepository.findByAccountIdOrderByPaymentDateDesc(accountId));
    }
}
