package com.hillayes.ftmarket.api.service;

import com.hillayes.shares.api.domain.PriceData;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@RequiredArgsConstructor
public class PriceLookupServiceTest {
    private final PriceLookupService priceLookupService;

    @ParameterizedTest
    @ValueSource(strings = { "GB00B0CNGT73", "TW." })
    public void testGetPrices(String symbol) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(20);

        Optional<List<PriceData>> prices = priceLookupService.getPrices(symbol, startDate, endDate);
        assertNotNull(prices);
        assertTrue(prices.isPresent());
        assertFalse(prices.get().isEmpty());

        AtomicReference<LocalDate> prev = new AtomicReference<>(null);
        prices.get().forEach( price -> {
            LocalDate prevDate = prev.get();
            if (prevDate != null) {
                // prices should be in date order ascending
                assertTrue(price.date().isAfter(prevDate));

                // prices should not include weekends
                // prices should not include weekends
                DayOfWeek dayOfWeek = prevDate.getDayOfWeek();
                assertNotEquals(DayOfWeek.SATURDAY, dayOfWeek);
                assertNotEquals(DayOfWeek.SUNDAY, dayOfWeek);
            }
            prev.set(price.date());
        });
    }
}
