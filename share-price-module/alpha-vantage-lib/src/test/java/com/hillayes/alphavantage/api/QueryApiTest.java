package com.hillayes.alphavantage.api;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class QueryApiTest {
    @Inject
    @RestClient
    QueryApi fixture;

    @Test
    public void testGetDaily() {
        Object response = fixture.getPriceHistory(TimeSeries.TIME_SERIES_DAILY, "TW.LON");

        assertNotNull(response);
    }
}
