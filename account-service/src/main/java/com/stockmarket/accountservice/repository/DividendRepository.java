package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.Dividend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DividendRepository extends JpaRepository<Dividend, String> {

    List<Dividend> findByAccountIdOrderByPaymentDateDesc(String accountId);

    List<Dividend> findByAccountIdAndPaymentDateBetweenOrderByPaymentDateAsc(
            String accountId, LocalDate from, LocalDate to);

    List<Dividend> findBySymbol(String symbol);

    @Query("""
            SELECT COALESCE(SUM(d.netAmount), 0)
            FROM Dividend d
            WHERE d.account.id = :accountId
            """)
    BigDecimal sumNetAmountByAccountId(@Param("accountId") String accountId);
}
