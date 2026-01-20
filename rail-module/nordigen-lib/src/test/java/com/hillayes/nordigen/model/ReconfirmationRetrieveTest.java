package com.hillayes.nordigen.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.hillayes.commons.json.MapperFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReconfirmationRetrieveTest {
    private final ObjectReader reader = MapperFactory.readerFor(ReconfirmationRetrieve.class);
    private final ObjectWriter writer = MapperFactory.writerFor(ReconfirmationRetrieve.class);

    @Test
    public void testSerialisation() throws JsonProcessingException {
        AccountConsent account1 = new AccountConsent();
        account1.reconfirmed = Instant.now();
        account1.rejected = Instant.now();

        AccountConsent account2 = new AccountConsent();
        account2.reconfirmed = Instant.now();
        account2.rejected = Instant.now();

        ReconfirmationRetrieve fixture = new ReconfirmationRetrieve();
        fixture.created = Instant.now();
        fixture.lastAccessed = Instant.now();
        fixture.lastSubmitted = Instant.now();
        fixture.reconfirmationUrl = "reconfirmationUrl";
        fixture.redirect = "redirect";
        fixture.accounts = Map.of(
            "account1", account1,
            "account2", account2
        );

        String json = writer.writeValueAsString(fixture);
        System.out.println(json);

        ReconfirmationRetrieve value = reader.readValue(json);

        assertEquals(fixture.created, value.created);
        assertEquals(fixture.lastAccessed, value.lastAccessed);
        assertEquals(fixture.lastSubmitted, value.lastSubmitted);
        assertEquals(fixture.reconfirmationUrl, value.reconfirmationUrl);
        assertEquals(fixture.redirect, value.redirect);

        assertEquals(fixture.accounts.size(), value.accounts.size());
        fixture.accounts.forEach((k, expected) -> {
            AccountConsent actual = value.accounts.get(k);
            assertNotNull(actual);

            assertEquals(expected.reconfirmed, actual.reconfirmed);
            assertEquals(expected.rejected, actual.rejected);
        });
    }
}
