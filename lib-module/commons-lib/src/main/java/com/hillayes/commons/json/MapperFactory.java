package com.hillayes.commons.json;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class MapperFactory {
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(new JavaTimeModule());

    public static ObjectMapper defaultMapper() {
        return objectMapper;
    }

    public static ObjectReader readerFor(Class<?> clazz) {
        JavaType type = TypeFactory.defaultInstance().constructType(clazz);
        return objectMapper.readerFor(type);
    }

    public static ObjectWriter writerFor(Class<?> clazz) {
        JavaType type = TypeFactory.defaultInstance().constructType(clazz);
        return objectMapper.writerFor(type);
    }
}
