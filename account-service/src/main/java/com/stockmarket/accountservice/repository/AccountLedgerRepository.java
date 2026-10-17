package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.AccountLedger;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AccountLedgerRepository extends JpaRepository<AccountLedger, String> {

    List<AccountLedger> findByAccountIdOrderByEntryNumberAsc(String accountId);

    /**
     * Highest entry number of an account, 0 when it has no entries yet.
     */
    @Query("SELECT COALESCE(MAX(l.entryNumber), 0) FROM AccountLedger l WHERE l.account.id = :accountId")
    long findMaxEntryNumber(@Param("accountId") String accountId);

    List<AccountLedger> findByTransactionDateBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Ledger entries of an account inside a period [from, to), in ledger order:
     * the order a running balance is computed in.
     */
    @Query("""
            SELECT l FROM AccountLedger l
            WHERE l.account.id = :accountId
              AND l.transactionDate >= :from
              AND l.transactionDate < :to
            ORDER BY l.entryNumber ASC
            """)
    List<AccountLedger> findForPeriod(@Param("accountId") String accountId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /**
     * Latest entry strictly before {@code before}; its {@code balanceAfter} is the
     * opening balance of a statement period. Call with {@code Limit.of(1)}.
     */
    @Query("""
            SELECT l FROM AccountLedger l
            WHERE l.account.id = :accountId
              AND l.transactionDate < :before
            ORDER BY l.entryNumber DESC
            """)
    List<AccountLedger> findLatestBefore(@Param("accountId") String accountId,
                                         @Param("before") LocalDateTime before,
                                         Limit limit);
}
