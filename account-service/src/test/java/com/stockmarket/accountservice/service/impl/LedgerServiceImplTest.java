package com.stockmarket.accountservice.service.impl;

import com.stockmarket.accountservice.entity.AccountLedger;
import com.stockmarket.accountservice.entity.InvestmentAccount;
import com.stockmarket.accountservice.enums.LedgerCategory;
import com.stockmarket.accountservice.enums.LedgerEntryType;
import com.stockmarket.accountservice.enums.ReferenceType;
import com.stockmarket.accountservice.exception.InsufficientBalanceException;
import com.stockmarket.accountservice.repository.AccountLedgerRepository;
import com.stockmarket.accountservice.service.model.LedgerStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceImplTest {

    @Mock
    private AccountLedgerRepository ledgerRepository;

    private LedgerServiceImpl service;
    private InvestmentAccount account;

    @BeforeEach
    void setUp() {
        service = new LedgerServiceImpl(ledgerRepository, null, null);
        account = new InvestmentAccount();
        account.setId("acc-1");
        account.setBalance(new BigDecimal("100.00"));
        lenient().when(ledgerRepository.findMaxEntryNumber(anyString())).thenReturn(4L);
        lenient().when(ledgerRepository.save(any(AccountLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creditIncreasesBalanceAndRecordsRunningBalance() {
        AccountLedger entry = service.postCredit(account, new BigDecimal("50"), LedgerCategory.DEPOSIT,
                ReferenceType.FUND_TRANSACTION, "tx-1", "Deposit");

        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("150.00");
        assertThat(entry.getEntryType()).isEqualTo(LedgerEntryType.CREDIT);
        assertThat(entry.getEntryNumber()).isEqualTo(5);
    }

    @Test
    void debitDecreasesBalance() {
        AccountLedger entry = service.postDebit(account, new BigDecimal("30.005"), LedgerCategory.FEE,
                ReferenceType.TRADE, "t-1", "Fees");

        // Amounts are rounded to cents before posting: 30.005 -> 30.01
        assertThat(entry.getAmount()).isEqualByComparingTo("30.01");
        assertThat(account.getBalance()).isEqualByComparingTo("69.99");
    }

    @Test
    void debitBeyondBalanceIsRejectedWithoutChanges() {
        assertThatThrownBy(() -> service.postDebit(account, new BigDecimal("100.01"), LedgerCategory.WITHDRAWAL,
                ReferenceType.FUND_TRANSACTION, "tx-2", "Withdrawal"))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        assertThatThrownBy(() -> service.postCredit(account, BigDecimal.ZERO, LedgerCategory.DEPOSIT,
                ReferenceType.FUND_TRANSACTION, "tx-3", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void runningBalanceStartsFromTheBalanceBeforeThePeriod() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = from.plusMonths(1);

        when(ledgerRepository.findLatestBefore(eq("acc-1"), eq(from), any(Limit.class)))
                .thenReturn(List.of(entry(3, LedgerEntryType.CREDIT, "500.00", "500.00")));
        when(ledgerRepository.findForPeriod("acc-1", from, to)).thenReturn(List.of(
                entry(4, LedgerEntryType.DEBIT, "120.00", "380.00"),
                entry(5, LedgerEntryType.CREDIT, "20.50", "400.50")));

        LedgerStatement statement = service.generateRunningBalance("acc-1", from, to);

        assertThat(statement.openingBalance()).isEqualByComparingTo("500.00");
        assertThat(statement.totalDebits()).isEqualByComparingTo("120.00");
        assertThat(statement.totalCredits()).isEqualByComparingTo("20.50");
        assertThat(statement.closingBalance()).isEqualByComparingTo("400.50");
        assertThat(statement.entries()).hasSize(2);
    }

    @Test
    void runningBalanceOfAnEmptyHistoryIsZero() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        when(ledgerRepository.findLatestBefore(eq("acc-1"), eq(from), any(Limit.class))).thenReturn(List.of());
        when(ledgerRepository.findForPeriod("acc-1", from, from.plusDays(1))).thenReturn(List.of());

        LedgerStatement statement = service.generateRunningBalance("acc-1", from, from.plusDays(1));

        assertThat(statement.openingBalance()).isEqualByComparingTo("0");
        assertThat(statement.closingBalance()).isEqualByComparingTo("0");
    }

    private static AccountLedger entry(long number, LedgerEntryType type, String amount, String balanceAfter) {
        return AccountLedger.builder()
                .entryNumber(number)
                .entryType(type)
                .category(type == LedgerEntryType.CREDIT ? LedgerCategory.DEPOSIT : LedgerCategory.WITHDRAWAL)
                .amount(new BigDecimal(amount))
                .balanceAfter(new BigDecimal(balanceAfter))
                .referenceType(ReferenceType.FUND_TRANSACTION)
                .referenceId("ref-" + number)
                .transactionDate(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build();
    }
}
