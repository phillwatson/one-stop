package com.hillayes.alphavantage.api.service;

import com.hillayes.alphavantage.api.domain.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

//@Disabled
@QuarkusTest
//@RequiredArgsConstructor
public class QueryApiTest {
    @ConfigProperty(name = "one-stop.alpha-vantage-api.secret-key", defaultValue = "not-set")
    String API_KEY;

    @Inject AlphaVantageApi fixture;

    @ParameterizedTest
    @ValueSource(strings = { "TW.LON" })
    public void testGetDaily(String ticker) {
        DailyTimeSeries response = fixture.getDailySeries(API_KEY, ApiFunction.TIME_SERIES_DAILY, ticker);

        assertNotNull(response);
        assertNotNull(response.series);
        assertFalse(response.series.isEmpty());
//        response.series.entrySet().stream()
//            .sorted(Map.Entry.comparingByKey())
//            .forEach(entry -> {
//                System.out.println("" + entry.getKey() + ": " + entry.getValue().close);
//            });
    }

    @ParameterizedTest
    @ValueSource(strings = { "TW.", "TW.LON" })
    public void testSymbolSearch(String ticker) {
        TickerSearchResponse response = fixture.symbolSearch(API_KEY, ApiFunction.SYMBOL_SEARCH, ticker);

        assertNotNull(response);
        assertNotNull(response.bestMatches);
        assertFalse(response.bestMatches.isEmpty());
        assertEquals(1, response.bestMatches.size());

        TickerSearchRecord record = response.bestMatches.get(0);
        assertEquals("TW.LON", record.symbol);
        assertEquals("Taylor Wimpey PLC", record.name);
        assertEquals("GBX", record.currency);
    }

    @Test
    public void testSymbolSearch_NoMatch() {
        TickerSearchResponse response = fixture.symbolSearch(API_KEY, ApiFunction.SYMBOL_SEARCH, "ZZ.LON");

        assertNotNull(response);
        assertNotNull(response.bestMatches);
        assertTrue(response.bestMatches.isEmpty());
    }
}
