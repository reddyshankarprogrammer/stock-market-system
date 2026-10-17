package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.AccountCharge;
import com.stockmarket.accountservice.enums.ChargeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AccountChargeRepository extends JpaRepository<AccountCharge, String> {

    List<AccountCharge> findByAccountIdOrderByChargeDateDesc(String accountId);

    /**
     * Total per charge type in [from, to] (inclusive days).
     */
    @Query("""
            SELECT c.chargeType AS type, SUM(c.amount) AS total
            FROM AccountCharge c
            WHERE c.account.id = :accountId
              AND c.chargeDate BETWEEN :from AND :to
            GROUP BY c.chargeType
            """)
    List<TypeTotal<ChargeType>> sumByTypeForPeriod(@Param("accountId") String accountId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM AccountCharge c WHERE c.account.id = :accountId")
    BigDecimal sumByAccountId(@Param("accountId") String accountId);

    /**
     * Removes everything a statement imported (used on delete and reprocess).
     */
    @Modifying
    @Query("DELETE FROM AccountCharge c WHERE c.statement.id = :statementId")
    int deleteByStatementId(@Param("statementId") String statementId);

    @Modifying
    @Query("DELETE FROM AccountCharge c WHERE c.trade.id IN :tradeIds")
    int deleteByTradeIdIn(@Param("tradeIds") List<String> tradeIds);
}
