package com.hillayes.commons;

import com.hillayes.commons.jpa.CurrencyConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
@Embeddable
public class MonetaryAmount {
    public static final MonetaryAmount ZERO = MonetaryAmount.of("GBP", 0);

    private long amount;

    @Convert(converter = CurrencyConverter.class)
    private Currency currency;

    public static MonetaryAmount of(String currencyStr, double amount) {
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

    public Double toDecimal() {
        return BigDecimal.valueOf(amount)
            .movePointLeft(currency.getDefaultFractionDigits())
            .doubleValue();
    }

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }
}
