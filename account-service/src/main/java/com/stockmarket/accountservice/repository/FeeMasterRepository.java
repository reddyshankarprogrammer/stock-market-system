package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.FeeMaster;
import com.stockmarket.accountservice.enums.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeeMasterRepository extends JpaRepository<FeeMaster, String> {

    /**
     * The active fee for a type. The schedule keeps one active row per type.
     */
    Optional<FeeMaster> findFirstByFeeTypeAndActiveTrue(FeeType feeType);

    List<FeeMaster> findByActiveTrueOrderByFeeType();
}
