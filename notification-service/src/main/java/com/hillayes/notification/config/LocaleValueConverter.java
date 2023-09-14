package com.hillayes.notification.config;

import com.hillayes.commons.Strings;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.Locale;

/**
 * Converts configuration properties of type Locale.
 */
public class LocaleValueConverter implements Converter<Locale> {
    @Override
    public Locale convert(String value) throws IllegalArgumentException, NullPointerException {
        return Strings.isBlank(value) ? null : Locale.forLanguageTag(value);
    }
}
