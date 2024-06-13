package com.hillayes.commons.json;

import com.hillayes.commons.Strings;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@Provider
public class InstantConverter implements ParamConverter<Instant> {
    public Instant fromString(String value){
        try {
            if (!Strings.isBlank(value)) {
                return Instant.parse(value);
            }
        } catch (DateTimeParseException e) {
        }
        throw new IllegalArgumentException("Invalid Instant date time format: " + value);
    }

    public String toString(Instant value){
        return (value == null) ? null : value.toString();
    }
}
