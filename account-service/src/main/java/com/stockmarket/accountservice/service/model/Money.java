package com.stockmarket.accountservice.service.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rounding rules for money, kept in one place.
 *
 * <ul>
 *   <li>Amounts (balances, fees, taxes, trade values): 2 decimals, HALF_UP.</li>
 *   <li>Platform execution prices: 4 decimals, HALF_UP.</li>
 *   <li>Quantities and average prices: 8 decimals (fractional crypto and forex units).</li>
 * </ul>
 */
public final class Money {

    public static final int AMOUNT_SCALE = 2;
    public static final int PRICE_SCALE = 4;
    public static final int QUANTITY_SCALE = 8;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Money() {
    }

    public static BigDecimal amount(BigDecimal value) {
        return value.setScale(AMOUNT_SCALE, ROUNDING);
    }

    public static BigDecimal price(BigDecimal value) {
        return value.setScale(PRICE_SCALE, ROUNDING);
    }

    /**
     * {@code percent}% of {@code base}, rounded to 2 decimals.
     */
    public static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        return amount(base.multiply(percent).divide(HUNDRED, 10, ROUNDING));
    }

    /**
     * {@code part} as a percentage of {@code whole}, rounded to 2 decimals; zero when whole is zero.
     */
    public static BigDecimal percentage(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return amount(BigDecimal.ZERO);
        }
        return amount(part.multiply(HUNDRED).divide(whole, 10, ROUNDING));
    }

    public static BigDecimal zero() {
        return amount(BigDecimal.ZERO);
    }
}
