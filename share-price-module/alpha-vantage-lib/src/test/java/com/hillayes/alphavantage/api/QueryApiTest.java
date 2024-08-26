package com.hillayes.alphavantage.api;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

//@QuarkusTest
public class QueryApiTest {
    //@Inject
    AlphaVantageApi fixture;

    //@Test
    public void testGetDaily() {
        DailyTimeSeries response = fixture.getDailySeries("TW.LON");

        assertNotNull(response);
        response.series.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                System.out.println("" + entry.getKey() + ": " + entry.getValue().close);
            });
    }
}
