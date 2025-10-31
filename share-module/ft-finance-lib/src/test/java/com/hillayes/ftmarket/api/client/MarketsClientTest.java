package com.hillayes.ftmarket.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.ftmarket.api.domain.IsinIssueLookup;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class MarketsClientTest {
    private ObjectMapper objectMapper = new ObjectMapper();
    private MarketsClient marketsClient = new MarketsClient(objectMapper, "https://markets.ft.com");

    @Test
    public void testAll() {
        List.of(
            IsinIssueLookup.builder().isin("GB00B80QG052").issueId("535631580").name("HSBC FTSE 250 Index Accumulation C").currencyCode("GBP").build(),
            IsinIssueLookup.builder().isin("GB00B0CNGR59").issueId("74137488").name("Legal & General European Index Trust I Class Accumulation").currencyCode("GBP").build(),
            IsinIssueLookup.builder().isin("GB00B0CNH502").issueId("74137467").name("Legal & General UK 100 Index Trust I Class Accumulation").currencyCode("GBP").build(),
            IsinIssueLookup.builder().isin("GB00B0CNGT73").issueId("74137468").name("Legal & General US Index Trust I Class Accumulation").currencyCode("GBP").build(),
            IsinIssueLookup.builder().isin("GB00B8XYYQ86").issueId("52606664").name("Royal London Short Term Money Market Fund Y Acc").currencyCode("GBP").build(),
            IsinIssueLookup.builder().isin("GB00BPQY8M80").issueId("54712").name("Aviva PLC").currencyCode("GBP").build(),
            IsinIssueLookup.builder().isin("GB0005603997").issueId("183100").name("Legal & General Group PLC").currencyCode("GBP").build(),
            IsinIssueLookup.builder().isin("GB00BDR05C01").issueId("2534902").name("National Grid PLC").currencyCode("GBP").build(),
            IsinIssueLookup.builder().isin("GB0008782301").issueId("274273").name("Taylor Wimpey PLC").currencyCode("GBP").build()
        ).forEach(expected -> {
            marketsClient.getIssueID(expected.getIsin()).ifPresentOrElse(lookup -> {
                assertNotNull(lookup.getIssueId());
                assertEquals(expected.getIsin(), lookup.getIsin());
                assertEquals(expected.getName(), lookup.getName());
                assertEquals(expected.getCurrencyCode(), lookup.getCurrencyCode());

            }, () -> fail("Missing " + expected.getIsin()));
        });
    }

    @Test
    public void testGetPrices() {
        String issueId = "183100"; // LGEN
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

            assertNotNull(price.open());
            assertNotNull(price.close());
            assertNotNull(price.low());
            assertNotNull(price.high());
            assertNotNull(price.volume());
            assertTrue(price.volume() > 0);
        });
    }
}
