package com.hillayes.commons.json;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InstantConverterTest {
    private final InstantConverter fixture = new InstantConverter();

    @Test
    public void testFromStringHappyPath() {
        // Given: a value Instant string
        String value = "2022-06-21T15:20:39Z";

        // When: the value is converted to Instant
        Instant result = fixture.fromString(value);

        // Then: the conversion is the same as the original value
        assertNotNull(result);
        Instant expected = Instant.parse(value);
        assertEquals(expected, result);
    }

    @Test
    public void testFromStringEmptyString() {
        // Given: an empty Instant string
        // When: the value is converted to Instant
        // Then: an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fixture.fromString(""));
    }

    @Test
    public void testFromStringBlankString() {
        // Given: an empty Instant string
        // When: the value is converted to Instant
        // Then: an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fixture.fromString("   "));
    }

    @Test
    public void testFromStringNullString() {
        // Given: an empty Instant string
        // When: the value is converted to Instant
        // Then: an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fixture.fromString(null));
    }

    @Test
    public void testFromStringInvalidString() {
        // Given: an empty Instant string
        // When: the value is converted to Instant
        // Then: an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fixture.fromString("2020-06-21"));
    }

    @Test
    public void testToStringHappyPath() {
        // Given: a value Instant
        Instant value = Instant.parse("2022-06-21T15:20:39Z");

        // When: the value is converted to string
        String result = fixture.toString(value);

        // Then: the conversion is the same as the original value
        assertEquals("2022-06-21T15:20:39Z", result);
    }
}
