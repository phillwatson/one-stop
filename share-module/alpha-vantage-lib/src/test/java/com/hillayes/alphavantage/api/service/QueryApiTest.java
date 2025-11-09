package com.hillayes.alphavantage.api.service;

import com.hillayes.alphavantage.api.domain.DailyTimeSeries;
import com.hillayes.alphavantage.api.domain.Overview;
import com.hillayes.alphavantage.api.domain.TickerSearchRecord;
import com.hillayes.alphavantage.api.domain.TickerSearchResponse;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@QuarkusTest
@RequiredArgsConstructor
public class QueryApiTest {
    private final AlphaVantageApi fixture;

    @ParameterizedTest
    @ValueSource(strings = { "TW.LON" })
    public void testGetDaily(String ticker) {
        DailyTimeSeries response = fixture.getDailySeries(ticker);

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
        TickerSearchResponse response = fixture.symbolSearch(ticker);

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
        TickerSearchResponse response = fixture.symbolSearch("ZZ.LON");

        assertNotNull(response);
        assertNotNull(response.bestMatches);
        assertTrue(response.bestMatches.isEmpty());
    }
}
