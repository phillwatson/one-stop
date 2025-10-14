package com.hillayes.alphavantage.api.service;

import com.hillayes.alphavantage.api.domain.DailyTimeSeries;
import com.hillayes.alphavantage.api.domain.Overview;
import com.hillayes.alphavantage.api.domain.TickerSearchResponse;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@RequiredArgsConstructor
public class QueryApiTest {
    private final AlphaVantageApi fixture;

    @Test
    public void testGetDaily() {
        DailyTimeSeries response = fixture.getDailySeries("TW.LON");

        assertNotNull(response);
        assertNotNull(response.series);
        response.series.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                System.out.println("" + entry.getKey() + ": " + entry.getValue().close);
            });
    }

    @Test
    public void testOverview() {
        Overview response = fixture.getOverview("TW.LON");

        assertNotNull(response);
        System.out.println(response);
    }

    @Test
    public void testSymbolSearch() {
        TickerSearchResponse response = fixture.symbolSearch("TW.L");

        assertNotNull(response);
        System.out.println(response);
    }
}
