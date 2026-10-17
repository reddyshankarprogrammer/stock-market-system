package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.StatementImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StatementImportHistoryRepository extends JpaRepository<StatementImportHistory, String> {

    List<StatementImportHistory> findByStatementIdOrderByAttemptNumberDesc(String statementId);

    Optional<StatementImportHistory> findFirstByStatementIdOrderByAttemptNumberDesc(String statementId);

    @Query("SELECT COALESCE(MAX(h.attemptNumber), 0) FROM StatementImportHistory h WHERE h.statement.id = :statementId")
    int findMaxAttemptNumber(@Param("statementId") String statementId);

    @Modifying
    @Query("DELETE FROM StatementImportHistory h WHERE h.statement.id = :statementId")
    int deleteByStatementId(@Param("statementId") String statementId);
}
