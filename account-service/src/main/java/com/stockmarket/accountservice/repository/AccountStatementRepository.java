package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.AccountStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountStatementRepository extends JpaRepository<AccountStatement, String> {

    List<AccountStatement> findByAccountIdOrderByUploadedAtDesc(String accountId);

    /**
     * Same file already uploaded to this account (detected by SHA-256 checksum).
     */
    Optional<AccountStatement> findByAccountIdAndChecksumSha256(String accountId, String checksumSha256);
}
