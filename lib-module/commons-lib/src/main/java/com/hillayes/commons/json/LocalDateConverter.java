package com.hillayes.commons.json;

import com.hillayes.commons.Strings;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Provider
public class LocalDateConverter implements ParamConverter<LocalDate> {
    public LocalDate fromString(String value){
        try {
            if (!Strings.isBlank(value)) {
                return LocalDate.parse(value);
            }
        } catch (DateTimeParseException e) {
        }
        throw new IllegalArgumentException("Invalid Local date time format: " + value);
    }

    public String toString(LocalDate value){
        return (value == null) ? null : value.toString();
    }
}
