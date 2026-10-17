package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.AccountTax;
import com.stockmarket.accountservice.enums.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AccountTaxRepository extends JpaRepository<AccountTax, String> {

    List<AccountTax> findByAccountIdOrderByChargedAtDesc(String accountId);

    List<AccountTax> findByTradeId(String tradeId);

    /**
     * Total per tax type in [from, to).
     */
    @Query("""
            SELECT t.taxType AS type, SUM(t.taxAmount) AS total
            FROM AccountTax t
            WHERE t.account.id = :accountId
              AND t.chargedAt >= :from
              AND t.chargedAt < :to
            GROUP BY t.taxType
            """)
    List<TypeTotal<TaxType>> sumByTypeForPeriod(@Param("accountId") String accountId,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    /**
     * Total per calendar month (1-12) in [from, to).
     */
    @Query("""
            SELECT EXTRACT(MONTH FROM t.chargedAt) AS month, SUM(t.taxAmount) AS total
            FROM AccountTax t
            WHERE t.account.id = :accountId
              AND t.chargedAt >= :from
              AND t.chargedAt < :to
            GROUP BY EXTRACT(MONTH FROM t.chargedAt)
            ORDER BY EXTRACT(MONTH FROM t.chargedAt)
            """)
    List<MonthTotal> sumByMonthForPeriod(@Param("accountId") String accountId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    @Modifying
    @Query("DELETE FROM AccountTax t WHERE t.statement.id = :statementId")
    int deleteByStatementId(@Param("statementId") String statementId);

    @Modifying
    @Query("DELETE FROM AccountTax t WHERE t.trade.id IN :tradeIds")
    int deleteByTradeIdIn(@Param("tradeIds") List<String> tradeIds);

    /**
     * Projection for the monthly tax summary.
     */
    interface MonthTotal {

        Integer getMonth();

        BigDecimal getTotal();
    }

    @Query("""
            SELECT COALESCE(SUM(t.taxAmount), 0)
            FROM AccountTax t
            WHERE t.account.id = :accountId
            """)
    BigDecimal sumTaxesByAccountId(@Param("accountId") String accountId);

    @Query("""
            SELECT COALESCE(SUM(t.taxAmount), 0)
            FROM AccountTax t
            WHERE t.account.id = :accountId
              AND t.chargedAt >= :from
              AND t.chargedAt < :to
            """)
    BigDecimal sumTaxesByAccountIdForPeriod(@Param("accountId") String accountId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);
}
