package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.dto.request.CreateAccountRequest;
import com.stockmarket.accountservice.dto.request.UpdateAccountRequest;
import com.stockmarket.accountservice.dto.response.AccountResponse;
import com.stockmarket.accountservice.dto.response.AccountSummaryResponse;
import com.stockmarket.accountservice.dto.response.AuditLogResponse;
import com.stockmarket.accountservice.dto.response.HoldingResponse;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.enums.AccountStatus;
import com.stockmarket.accountservice.enums.AuditAction;
import com.stockmarket.accountservice.enums.BrokerName;
import com.stockmarket.accountservice.enums.FundTransactionType;
import com.stockmarket.accountservice.exception.AccountAccessDeniedException;
import com.stockmarket.accountservice.exception.ValidationException;
import com.stockmarket.accountservice.mapper.AccountMapper;
import com.stockmarket.accountservice.repository.AccountFeeRepository;
import com.stockmarket.accountservice.repository.AccountTaxRepository;
import com.stockmarket.accountservice.repository.DividendRepository;
import com.stockmarket.accountservice.repository.FundTransactionRepository;
import com.stockmarket.accountservice.repository.HoldingRepository;
import com.stockmarket.accountservice.repository.InterestRepository;
import com.stockmarket.accountservice.repository.InvestmentAccountRepository;
import com.stockmarket.accountservice.repository.TradeRepository;
import com.stockmarket.accountservice.service.AccountAccessService;
import com.stockmarket.accountservice.service.AccountService;
import com.stockmarket.accountservice.service.AuditService;
import com.stockmarket.accountservice.security.RequestUser;
import com.stockmarket.accountservice.service.HoldingService;
import com.stockmarket.accountservice.service.UserValidationService;
import com.stockmarket.accountservice.service.model.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final String ACCOUNT_NUMBER_PREFIX = "INV";
    private static final int ACCOUNT_NUMBER_DIGITS = 12;
    private static final int ACCOUNT_NUMBER_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();

    private final InvestmentAccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final FundTransactionRepository fundTransactionRepository;
    private final AccountFeeRepository accountFeeRepository;
    private final AccountTaxRepository accountTaxRepository;
    private final DividendRepository dividendRepository;
    private final InterestRepository interestRepository;
    private final AccountAccessService accountAccessService;
    private final HoldingService holdingService;
    private final AuditService auditService;
    private final UserValidationService userValidationService;
    private final AccountMapper mapper;

    /**
     * The user must exist in user-service and be ACTIVE. The owner is always the caller
     * (X-User-Id); a user id is never taken from the request body.
     */
    @Override
    @Transactional
    public AccountResponse createAccount(String userId, CreateAccountRequest request) {

        userValidationService.requireActiveCurrentUser();

        InvestmentAccount account = InvestmentAccount.builder()
                .accountNumber(generateAccountNumber())
                .userId(userId)
                .accountName(request.getAccountName().trim())
                .accountType(request.getAccountType())
                .brokerName(request.getBrokerName() == null ? BrokerName.PLATFORM : request.getBrokerName())
                .currency(request.getCurrency() == null ? DEFAULT_CURRENCY : request.getCurrency())
                .remarks(request.getRemarks())
                .balance(Money.zero())
                .status(AccountStatus.ACTIVE)
                .build();

        InvestmentAccount saved = accountRepository.save(account);
        AccountResponse response = mapper.toAccountResponse(saved);

        auditService.logAccountAction(saved.getId(), AuditAction.CREATE,
                "Opened " + saved.getBrokerName() + " " + saved.getAccountType() + " account " + saved.getAccountNumber(),
                request, response);
        log.info("Account {} ({}) opened for user {}", saved.getAccountNumber(), saved.getBrokerName(), userId);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId, String userId) {
        return mapper.toAccountResponse(accountAccessService.getOwnedAccount(accountId, userId));
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(String accountId, String userId, UpdateAccountRequest request) {

        InvestmentAccount account = accountAccessService.getOwnedAccount(accountId, userId);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new ValidationException("A closed account cannot be changed");
        }

        if (!account.getUserId().equals(userId)) {
            // Admins can read any account but only owners change one.
            throw AccountAccessDeniedException.forAccount(accountId);
        }

        String oldName = account.getAccountName();
        account.setAccountName(request.getAccountName().trim());
        if (request.getRemarks() != null) {
            account.setRemarks(request.getRemarks());
        }
        InvestmentAccount saved = accountRepository.save(account);
        AccountResponse response = mapper.toAccountResponse(saved);

        auditService.logAccountAction(accountId, AuditAction.UPDATE,
                "Renamed from '" + oldName + "' to '" + saved.getAccountName() + "'", request, response);
        return response;
    }

    @Override
    @Transactional
    public AccountResponse deleteAccount(String accountId, String userId) {

        InvestmentAccount account = accountAccessService.getOwnedActiveAccountForUpdate(accountId, userId);

        if (account.getBalance().signum() != 0) {
            throw new ValidationException("Withdraw the remaining balance of "
                    + account.getBalance().toPlainString() + " before closing the account");
        }
        if (holdingRepository.existsByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)) {
            throw new ValidationException("Sell all holdings before closing the account");
        }

        account.setStatus(AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        InvestmentAccount saved = accountRepository.save(account);

        auditService.logAccountAction(accountId, AuditAction.CLOSE, "Closed account " + saved.getAccountNumber());
        log.info("Account {} closed", saved.getAccountNumber());

        return mapper.toAccountResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(String userId, String callerId) {

        if (!userId.equals(callerId)) {
            if (!RequestUser.isAdmin()) {
                throw AccountAccessDeniedException.forUser(userId);
            }
            // An admin may look up any user, but only one that exists.
            userValidationService.getUser(userId);
        }
        return mapper.toAccountResponses(accountRepository.findByUserId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountSummaryResponse getSummary(String accountId, String userId) {

        InvestmentAccount account = accountAccessService.getOwnedAccount(accountId, userId);
        List<HoldingResponse> holdings = holdingService.valueOpenPositions(accountId);

        BigDecimal invested = sum(holdings.stream().map(HoldingResponse::getTotalCost).toList());
        // Without a live price, value the position at cost so the totals stay meaningful.
        BigDecimal marketValue = sum(holdings.stream()
                .map(h -> h.getMarketValue() != null ? h.getMarketValue() : h.getTotalCost())
                .toList());
        boolean pricesLive = holdings.stream().allMatch(h -> h.getCurrentPrice() != null);

        return AccountSummaryResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .cashBalance(account.getBalance())
                .openPositions(holdings.size())
                .investedAmount(invested)
                .marketValue(marketValue)
                .unrealizedProfit(Money.amount(marketValue.subtract(invested)))
                .realizedProfit(Money.amount(tradeRepository.sumRealizedProfitByAccountId(accountId)))
                .totalEquity(Money.amount(account.getBalance().add(marketValue)))
                .totalDeposits(Money.amount(fundTransactionRepository
                        .sumAmountByAccountIdAndType(accountId, FundTransactionType.DEPOSIT)))
                .totalWithdrawals(Money.amount(fundTransactionRepository
                        .sumAmountByAccountIdAndType(accountId, FundTransactionType.WITHDRAWAL)))
                .totalFees(Money.amount(accountFeeRepository.sumFeesByAccountId(accountId)))
                .totalTaxes(Money.amount(accountTaxRepository.sumTaxesByAccountId(accountId)))
                .totalDividends(Money.amount(dividendRepository.sumNetAmountByAccountId(accountId)))
                .totalInterest(Money.amount(interestRepository.sumInterestByAccountId(accountId)))
                .pricesLive(pricesLive)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditTrail(String accountId, String userId) {

        accountAccessService.getOwnedAccount(accountId, userId);
        return auditService.getAccountAuditTrail(accountId);
    }

    /**
     * {@code INV} + 12 random digits; retried on the (unlikely) collision.
     */
    private String generateAccountNumber() {

        for (int attempt = 0; attempt < ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            StringBuilder number = new StringBuilder(ACCOUNT_NUMBER_PREFIX);
            for (int i = 0; i < ACCOUNT_NUMBER_DIGITS; i++) {
                number.append(random.nextInt(10));
            }
            if (!accountRepository.existsByAccountNumber(number.toString())) {
                return number.toString();
            }
        }
        throw new IllegalStateException("Could not generate a unique account number");
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .reduce(Money.zero(), BigDecimal::add);
    }
}
