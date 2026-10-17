package com.stockmarket.accountservice.repository;

import com.stockmarket.accountservice.entity.TaxMaster;
import com.stockmarket.accountservice.enums.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxMasterRepository extends JpaRepository<TaxMaster, String> {

    /**
     * Active rules of a type. TRADE_TAX may have one rule per side, so this returns a list.
     */
    List<TaxMaster> findByTaxTypeAndActiveTrue(TaxType taxType);

    List<TaxMaster> findByActiveTrueOrderByTaxType();
}
