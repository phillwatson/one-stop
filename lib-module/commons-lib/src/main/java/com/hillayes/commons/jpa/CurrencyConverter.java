package com.hillayes.commons.jpa;

import com.hillayes.commons.Strings;
import jakarta.persistence.AttributeConverter;

import java.util.Currency;

/**
 * Allows Currency objects to be persisted as strings in the database.
 */
public class CurrencyConverter implements AttributeConverter<Currency, String> {
    @Override
    public String convertToDatabaseColumn(Currency currency) {
        return (currency == null) ? null : currency.getCurrencyCode();
    }

    @Override
    public Currency convertToEntityAttribute(String value) {
        return Strings.isBlank(value) ? null : Currency.getInstance(value);
    }
}
