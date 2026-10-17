package com.stockmarket.accountservice.repository;

import java.math.BigDecimal;

/**
 * Projection for "total per type" aggregate queries.
 *
 * @param <T> the enum the rows are grouped by
 */
public interface TypeTotal<T extends Enum<T>> {

    T getType();

    BigDecimal getTotal();
}
