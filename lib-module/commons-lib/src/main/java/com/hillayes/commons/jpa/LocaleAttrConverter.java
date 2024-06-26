package com.hillayes.commons.jpa;

import com.hillayes.commons.Strings;
import jakarta.persistence.AttributeConverter;

import java.util.Locale;

/**
 * Allows Locale objects to be persisted as strings in the database.
 */
public class LocaleAttrConverter implements AttributeConverter<Locale, String> {
    @Override
    public String convertToDatabaseColumn(Locale locale) {
        return (locale == null) ? null : locale.toLanguageTag();
    }

    @Override
    public Locale convertToEntityAttribute(String value) {
        return Strings.isBlank(value) ? null : Locale.forLanguageTag(value);
    }
}
