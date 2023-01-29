package com.hillayes.rail.model;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.Provider;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Provider
public class LocalDateConverter implements ParamConverter<LocalDate> {
    public LocalDate fromString(String value){
        try {
            if ((value == null) || (value.isBlank())) {
                String x = LocalDate.parse(value).toString();
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