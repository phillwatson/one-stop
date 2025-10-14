package com.hillayes.shares.api.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Used to convey share/fund prices from the Share Provider to the Share Service.
 * @param date The date to which the price data applies.
 * @param open The opening price on the date.
 * @param high The highest price achieved on the date.
 * @param low The lowest price achieved on the date.
 * @param close The price at market close on the date.
 */
public record PriceData(
    LocalDate date,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close
) {}
