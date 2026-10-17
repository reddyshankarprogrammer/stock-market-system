package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.request.InterestRequest;
import com.stockmarket.accountservice.dto.response.InterestResponse;
import com.stockmarket.accountservice.entity.InterestTransaction;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.enums.AuditAction;
import com.stockmarket.accountservice.enums.LedgerCategory;
import com.stockmarket.accountservice.enums.ReferenceType;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.InterestRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.AuditService;
import com.stockmarket.accountservice.service.InterestService;
import com.stockmarket.accountservice.service.LedgerService;
import com.stockmarket.accountservice.service.model.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {

    private static final BigDecimal DAYS_PER_YEAR = BigDecimal.valueOf(365);

    private final InterestRepository interestRepository;
    private final AccountAccessService accountAccessService;
    private final LedgerService ledgerService;
    private final AuditService auditService;
    private final AccountMapper mapper;

    /**
     * Simple interest on the current balance: balance x rate% x days / 365.
     */
    @Override
    @Transactional
    public InterestResponse postInterest(String accountId, InterestRequest request) {

        if (request.getPeriodEnd().isBefore(request.getPeriodStart())) {
            throw new ValidationException("Period end must not be before period start");
        }

        InvestmentAccount account = accountAccessService.getActiveAccountForUpdate(accountId);

        if (interestRepository.existsOverlappingPeriod(accountId, request.getPeriodStart(), request.getPeriodEnd())) {
            throw new ValidationException("Interest was already posted for part of "
                    + request.getPeriodStart() + " to " + request.getPeriodEnd());
        }

        int days = (int) ChronoUnit.DAYS.between(request.getPeriodStart(), request.getPeriodEnd()) + 1;
        BigDecimal principal = account.getBalance();
        // Round once, at the end: principal x rate x days / (100 x 365).
        BigDecimal interest = Money.amount(principal
                .multiply(request.getAnnualRate())
                .multiply(BigDecimal.valueOf(days))
                .divide(DAYS_PER_YEAR.multiply(BigDecimal.valueOf(100)), 10, Money.ROUNDING));

        if (interest.signum() <= 0) {
            throw new ValidationException("No interest due: balance " + principal.toPlainString()
                    + " earns less than 0.01 for the period");
        }

        InterestTransaction transaction = interestRepository.save(InterestTransaction.builder()
                .account(account)
                .principalAmount(principal)
                .annualRate(request.getAnnualRate())
                .days(days)
                .interestAmount(interest)
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .postedAt(LocalDateTime.now())
                .build());

        ledgerService.postCredit(account, interest, LedgerCategory.INTEREST, ReferenceType.INTEREST, transaction.getId(),
                "Interest " + request.getPeriodStart() + " to " + request.getPeriodEnd()
                        + " at " + request.getAnnualRate().toPlainString() + "%");

        auditService.logAccountAction(accountId, AuditAction.INTEREST,
                "Interest " + interest.toPlainString() + " for " + days + " days");
        log.info("Interest {} posted to account {}", interest, account.getAccountNumber());

        return mapper.toInterestResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterestResponse> getInterest(String accountId, String userId) {

        accountAccessService.getOwnedAccount(accountId, userId);
        return mapper.toInterestResponses(interestRepository.findByAccountIdOrderByPostedAtDesc(accountId));
    }
}
