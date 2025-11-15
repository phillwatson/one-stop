package com.hillayes.notification.config;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LocaleValueConverterTest {
    @Test
    public void testConvert_null() {
        LocaleValueConverter fixture = new LocaleValueConverter();

        assertNull(fixture.convert(null));
    }

    @Test
    public void testConvert_blank() {
        LocaleValueConverter fixture = new LocaleValueConverter();

        assertNull(fixture.convert(""));
        assertNull(fixture.convert(" "));
    }

    @Test
    public void testConvert() {
        LocaleValueConverter fixture = new LocaleValueConverter();

        Locale[] locales = {
            Locale.ENGLISH, Locale.CHINESE, Locale.CANADA_FRENCH, Locale.FRENCH, Locale.GERMAN
        };
        for (Locale locale : locales) {
            assertEquals(locale, fixture.convert(locale.toLanguageTag()));
        }
    }
}
