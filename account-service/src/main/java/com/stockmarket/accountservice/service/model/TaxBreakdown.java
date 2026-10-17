package com.stockmarket.accountservice.service.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * All taxes for one trade or dividend.
 *
 * @param charges individual taxes; zero-amount taxes are left out
 * @param total   sum of the charges
 */
public record TaxBreakdown(List<TaxCharge> charges, BigDecimal total) {
}
