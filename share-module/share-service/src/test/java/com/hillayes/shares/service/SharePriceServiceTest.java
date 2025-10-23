package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.config.ShareProviderFactory;
import com.hillayes.shares.domain.PriceHistory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceResolution;
import com.hillayes.shares.repository.PriceHistoryRepository;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.utils.TestData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SharePriceServiceTest {
    private final ShareIndexRepository shareIndexRepository = mock();
    private final PriceHistoryRepository priceHistoryRepository = mock();
    private final ShareProviderFactory providerFactory = mock();

    private final SharePriceService fixture = new SharePriceService(
        shareIndexRepository,
        priceHistoryRepository,
        providerFactory
    );

    @Test
    public void testGetPrices() {
        // Given: A share index
        ShareIndex shareIndex = TestData.mockShareIndex(s -> s.id(UUID.randomUUID()));

        // And: the index has a collection of daily prices
        LocalDate startDate = LocalDate.now().minusDays(50);
        LocalDate today = LocalDate.now();
        LocalDate date = startDate;
        List<PriceHistory> prices = new ArrayList<>();
        while (date.isBefore(today)) {
            prices.add(TestData.mockPriceHistory(shareIndex, date, SharePriceResolution.DAILY));
            date = date.plusDays(1);
        }

        AtomicReference<List<PriceHistory>> subset = new AtomicReference<>();
        when(priceHistoryRepository.listPrices(
            eq(shareIndex), eq(SharePriceResolution.DAILY), any(LocalDate.class), any(LocalDate.class), anyInt(), anyInt()))
            .then(invocation -> {
                LocalDate fromDate = invocation.getArgument(2);
                LocalDate toDate = invocation.getArgument(3);
                int pageIndex = invocation.getArgument(4);
                int pageSize = invocation.getArgument(5);

                subset.set(prices.stream()
                    .filter(p -> (!p.getId().getDate().isBefore(fromDate)) && (p.getId().getDate().isBefore(toDate)))
                    .toList());
                return Page.of(subset.get(), pageIndex, pageSize);
            });


        // When: a page is retrieved
        Page<PriceHistory> pagedPrices = fixture.getPrices(
            shareIndex,
            today.minusDays(90),
            today.minusDays(5),
            3, 10);

        // Then: the result corresponds to the input
        assertNotNull(pagedPrices);
        assertEquals(subset.get().size(), pagedPrices.getTotalCount());
        assertEquals(5, pagedPrices.getTotalPages());

        assertEquals(10, pagedPrices.getContentSize());
        assertEquals(3, pagedPrices.getPageIndex());
        assertEquals(10, pagedPrices.getPageSize());
    }
}
