package com.hillayes.commons;

import com.hillayes.commons.jpa.CurrencyConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * An immutable value object representing a monetary amount.
 *
 * The amount is stored in the currency's minor units, as a long to avoid floating
 * point arithmetic; but it provides a method to convert the amount to a double
 * according to the fractional digits of its currency.
 *
 * MonetaryAmount is declared as a JPA embeddable class so that it can be used
 * as an attribute in an entity. The properties "amount" and "currency" should
 * be mapped to columns of types "bigint" and "varchar", respectively. The
 * currency is persisted as its ISO 4217 currency code.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
@Embeddable
public class MonetaryAmount {
    public static final MonetaryAmount ZERO = MonetaryAmount.of("GBP", 0);

    @Convert(converter = CurrencyConverter.class)
    private Currency currency;

    private long amount;

    /**
     * A factory method to create a new MonetaryAmount from a currency code and
     * an amount expressed in the currency's major units.
     * @param currencyStr the ISO 4217 currency code.
     * @param amount the amount.
     * @return a new MonetaryAmount instance.
     */
    public static MonetaryAmount of(String currencyStr, BigDecimal amount) {
        Currency currency = Currency.getInstance(currencyStr);
        long value = (amount == null) ? 0 : amount
            .movePointRight(currency.getDefaultFractionDigits())
            .longValue();

        return new MonetaryAmount(currency, value);
    }

    /**
     * A factory method to create a new MonetaryAmount from a currency code and
     * an amount expressed in the currency's major units.
     * @param currencyStr the ISO 4217 currency code.
     * @param amount the amount.
     * @return a new MonetaryAmount instance.
     */
    public static MonetaryAmount of(String currencyStr, double amount) {
        return of(currencyStr, BigDecimal.valueOf(amount));
    }

    /**
     * A factory method to create a new MonetaryAmount from a currency code and
     * an amount expressed in the currency's minor units.
     * @param currencyStr the ISO 4217 currency code.
     * @param amount the amount expressed in the currency's minor units.
     * @return a new MonetaryAmount instance.
     */
    public static MonetaryAmount of(String currencyStr, long amount) {
        return new MonetaryAmount(Currency.getInstance(currencyStr), amount);
    }

    /**
     * Returns the amount in the currency's major units, as a double, according
     * to the currency's fractional digits.
     */
    public Double toDecimal() {
        return BigDecimal.valueOf(amount)
            .movePointLeft(currency.getDefaultFractionDigits())
            .doubleValue();
    }

    /**
     * Returns the currency code.
     */
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }
}
