package com.stockmarket.accountservice.service.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * All fees for one trade or withdrawal.
 *
 * @param baseAmount amount the fees were calculated on
 * @param charges    individual fees; zero-amount fees are left out
 * @param total      sum of the charges
 */
public record FeeBreakdown(BigDecimal baseAmount, List<FeeCharge> charges, BigDecimal total) {
}
