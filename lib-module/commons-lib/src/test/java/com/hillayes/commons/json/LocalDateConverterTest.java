package com.hillayes.commons.json;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

public class LocalDateConverterTest {
    private final LocalDateConverter fixture = new LocalDateConverter();

    @Test
    public void testFromStringHappyPath() {
        // Given: a value LocalDate string
        String value = "2022-06-21";

        // When: the value is converted to LocalDate
        LocalDate result = fixture.fromString(value);

        // Then: the conversion is the same as the original value
        assertNotNull(result);
        assertEquals(2022, result.getYear());
        assertEquals(Month.JUNE, result.getMonth());
        assertEquals(21, result.getDayOfMonth());
    }

    @Test
    public void testFromStringEmptyString() {
        // Given: an empty LocalDate string
        // When: the value is converted to LocalDate
        // Then: an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fixture.fromString(""));
    }

    @Test
    public void testFromStringBlankString() {
        // Given: an empty LocalDate string
        // When: the value is converted to LocalDate
        // Then: an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fixture.fromString("   "));
    }

    @Test
    public void testFromStringNullString() {
        // Given: an empty LocalDate string
        // When: the value is converted to LocalDate
        // Then: an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fixture.fromString(null));
    }

    @Test
    public void testFromStringInvalidString() {
        // Given: an empty LocalDate string
        // When: the value is converted to LocalDate
        // Then: an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fixture.fromString("2020-06-21T12:00:00Z"));
    }

    @Test
    public void testToStringHappyPath() {
        // Given: a value LocalDate
        LocalDate value = LocalDate.parse("2022-06-21");

        // When: the value is converted to string
        String result = fixture.toString(value);

        // Then: the conversion is the same as the original value
        assertEquals("2022-06-21", result);
    }
}
