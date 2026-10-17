package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.AccountFundTransaction;
import com.stockmarket.accountservice.enums.FundTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FundTransactionRepository extends JpaRepository<AccountFundTransaction, String> {

    List<AccountFundTransaction> findByAccountIdOrderByTransactionDateDesc(String accountId);

    List<AccountFundTransaction> findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAsc(
            String accountId, LocalDateTime from, LocalDateTime to);

    List<AccountFundTransaction> findByTransactionDateBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Total deposited or withdrawn on an account; zero when there are none.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM AccountFundTransaction t
            WHERE t.account.id = :accountId
              AND t.transactionType = :type
              AND t.status = com.stockmarket.accountservice.enums.TransactionStatus.COMPLETED
            """)
    BigDecimal sumAmountByAccountIdAndType(@Param("accountId") String accountId,
                                           @Param("type") FundTransactionType type);
}
