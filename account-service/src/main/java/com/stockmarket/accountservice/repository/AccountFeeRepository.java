package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.AccountFee;
import com.stockmarket.accountservice.enums.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AccountFeeRepository extends JpaRepository<AccountFee, String> {

    List<AccountFee> findByAccountIdOrderByChargedAtDesc(String accountId);

    List<AccountFee> findByTradeId(String tradeId);

    /**
     * Platform fee-engine totals per fee type in [from, to).
     */
    @Query("""
            SELECT f.feeType AS type, SUM(f.feeAmount) AS total
            FROM AccountFee f
            WHERE f.account.id = :accountId
              AND f.chargedAt >= :from
              AND f.chargedAt < :to
            GROUP BY f.feeType
            """)
    List<TypeTotal<FeeType>> sumByTypeForPeriod(@Param("accountId") String accountId,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(f.feeAmount), 0)
            FROM AccountFee f
            WHERE f.account.id = :accountId
            """)
    BigDecimal sumFeesByAccountId(@Param("accountId") String accountId);

    @Query("""
            SELECT COALESCE(SUM(f.feeAmount), 0)
            FROM AccountFee f
            WHERE f.account.id = :accountId
              AND f.chargedAt >= :from
              AND f.chargedAt < :to
            """)
    BigDecimal sumFeesByAccountIdForPeriod(@Param("accountId") String accountId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);
}
