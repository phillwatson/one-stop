package com.hillayes.shares.ft.client;

import com.hillayes.shares.api.domain.PriceData;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class MarketsClientTest {
    private final MarketsClient marketsClient = new MarketsClient();

    @Test
    public void testGetIssueId() {
        String stockIsin = "GB00B0CNGT73:GBP";

        Optional<String> issueId = marketsClient.getIssueID(stockIsin);

        assertNotNull(issueId);
        assertTrue(issueId.isPresent());
        assertEquals("74137468", issueId.get());
    }

    @Test
    public void testGetPrices() {
        String issueId = "74137468";
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(20);

        List<PriceData> prices = marketsClient.getPrices(issueId, startDate, endDate);

        assertNotNull(prices);
        assertFalse(prices.isEmpty());

        AtomicReference<LocalDate> prev = new AtomicReference<>(null);
        prices.forEach( price -> {
            LocalDate prevDate = prev.get();
            if (prevDate != null) {
                // prices should be in date order ascending
                assertTrue(price.date().isAfter(prevDate));

                // prices should not include weekends
                DayOfWeek preDayOfWeek = prevDate.getDayOfWeek();
                DayOfWeek expectedDay = (preDayOfWeek == DayOfWeek.FRIDAY) ? DayOfWeek.MONDAY : preDayOfWeek.plus(1);
                assertEquals(expectedDay, price.date().getDayOfWeek());
            }
            prev.set(price.date());
        });
    }
}
