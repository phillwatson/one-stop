package com.hillayes.rail.api.domain;

import lombok.*;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class MonetaryAmount {
    public static final MonetaryAmount ZERO = MonetaryAmount.of("GBP", 0);

    private long amount;

    private Currency currency;

    public static MonetaryAmount of(String currencyStr, float amount) {
        Currency currency = Currency.getInstance(currencyStr);
        long value = BigDecimal.valueOf(amount)
            .movePointRight(currency.getDefaultFractionDigits())
            .longValue();

        return MonetaryAmount.builder()
            .amount(value)
            .currency(currency)
            .build();
    }

    public static MonetaryAmount of(String currencyStr, long amount) {
        return MonetaryAmount.builder()
            .amount(amount)
            .currency(Currency.getInstance(currencyStr))
            .build();
    }

    public float toDecimal() {
        return BigDecimal.valueOf(amount)
            .movePointLeft(currency.getDefaultFractionDigits())
            .floatValue();
    }
}
