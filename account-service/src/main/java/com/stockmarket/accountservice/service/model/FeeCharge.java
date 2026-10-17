package com.stockmarket.accountservice.service.model;

import com.stockmarket.accountservice.entity.FeeMaster;

import java.math.BigDecimal;

/**
 * One fee computed by the fee engine.
 *
 * @param feeMaster  schedule entry that produced it
 * @param baseAmount amount it was calculated on
 * @param amount     fee, rounded to 2 decimals
 */
public record FeeCharge(FeeMaster feeMaster, BigDecimal baseAmount, BigDecimal amount) {
}
