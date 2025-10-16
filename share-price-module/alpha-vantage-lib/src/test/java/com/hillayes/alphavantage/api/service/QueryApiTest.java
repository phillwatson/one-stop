package com.hillayes.alphavantage.api.service;

import com.hillayes.alphavantage.api.domain.DailyTimeSeries;
import com.hillayes.alphavantage.api.domain.Overview;
import com.hillayes.alphavantage.api.domain.TickerSearchRecord;
import com.hillayes.alphavantage.api.domain.TickerSearchResponse;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@RequiredArgsConstructor
public class QueryApiTest {
    private final AlphaVantageApi fixture;

    @Test
    public void testGetDaily() {
        DailyTimeSeries response = fixture.getDailySeries("TW.LON");

        assertNotNull(response);
        assertNotNull(response.series);
        assertFalse(response.series.isEmpty());
//        response.series.entrySet().stream()
//            .sorted(Map.Entry.comparingByKey())
//            .forEach(entry -> {
//                System.out.println("" + entry.getKey() + ": " + entry.getValue().close);
//            });
    }

    @Test
    public void testOverview() {
        Overview response = fixture.getOverview("TW.LON");

        assertNotNull(response);
        assertNull(response.symbol);
        assertNull(response.name);
        assertNull(response.currency);
    }

    @Test
    public void testSymbolSearch() {
        TickerSearchResponse response = fixture.symbolSearch("TW.LON");

        assertNotNull(response);
        assertFalse(response.bestMatches.isEmpty());
        assertEquals(1, response.bestMatches.size());

        TickerSearchRecord record = response.bestMatches.get(0);
        assertEquals("TW.LON", record.symbol);
        assertEquals("Taylor Wimpey PLC", record.name);
        assertEquals("GBX", record.currency);

    }
}
