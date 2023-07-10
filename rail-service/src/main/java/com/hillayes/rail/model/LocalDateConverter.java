package com.hillayes.rail.model;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Provider
public class LocalDateConverter implements ParamConverter<LocalDate> {
    public LocalDate fromString(String value){
        try {
            if ((value != null) && (!value.isBlank())) {
                return LocalDate.parse(value);
            }
        } catch (DateTimeParseException e) {
        }
        return null;
    }

    public String toString(LocalDate value){
        return (value == null) ? null : value.toString();
    }
}
