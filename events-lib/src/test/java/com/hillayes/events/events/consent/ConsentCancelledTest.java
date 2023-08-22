package com.hillayes.events.events.consent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.json.MapperFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConsentCancelledTest {
    private final static ObjectMapper objectMapper = MapperFactory.defaultMapper();

    @Test
    public void testValidation() throws Exception {
        ConsentCancelled instance = ConsentCancelled.builder()
            .userId(null)
            .dateCancelled(null)
            .consentId(null)
            .institutionId(null)
            .agreementId(null)
            .agreementExpires(null)
            .requisitionId(null)
            .build();

        String json = objectMapper.writeValueAsString(instance);

        ConsentCancelled result = objectMapper.readValue(json, ConsentCancelled.class);
        assertNotNull(result);
    }
}
