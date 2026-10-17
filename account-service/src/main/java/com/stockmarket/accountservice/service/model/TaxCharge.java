package com.stockmarket.accountservice.service.model;

import com.stockmarket.accountservice.entity.TaxMaster;

import java.math.BigDecimal;

/**
 * One tax computed by the tax engine.
 *
 * @param taxMaster     rule that produced it
 * @param taxableAmount amount it was calculated on
 * @param amount        tax, rounded to 2 decimals
 */
public record TaxCharge(TaxMaster taxMaster, BigDecimal taxableAmount, BigDecimal amount) {
}
