package com.hillayes.events.serializers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class MapperFactory {
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModule(new JavaTimeModule());;

    public static ObjectMapper defaultMapper() {
        return objectMapper;
    }
}
