package com.hillayes.shares.ft.service;

import com.hillayes.shares.api.domain.PriceData;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@RequiredArgsConstructor
public class PriceLookupServiceTest {
    private final PriceLookupService priceLookupService;

    @Test
    public void testGetPrices() {
        String stockIsin = "GB00B0CNGT73";
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(20);

        List<PriceData> prices = priceLookupService.getPrices(stockIsin, startDate, endDate);
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
