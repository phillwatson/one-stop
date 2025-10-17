package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.PriceHistory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceResolution;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
@RequiredArgsConstructor
public class PriceHistoryRepositoryTest {
    private final PriceHistoryRepository priceHistoryRepository;

    @Test
    public void testListPrices() {
        // Given:
        ShareIndex shareIndex = ShareIndex.builder().id(UUID.randomUUID()).build();
        LocalDate fromDate = LocalDate.now().minusDays(20);
        LocalDate toDate = LocalDate.now().minusDays(2);

        // When:
        Page<PriceHistory> page = priceHistoryRepository
            .listPrices(shareIndex, SharePriceResolution.DAILY, fromDate, toDate, 2, 20);

        // Then:
        assertNotNull(page);
        assertTrue(page.isEmpty());
        assertEquals(0, page.getContentSize());
        assertEquals(2, page.getPageIndex());
        assertEquals(20, page.getPageSize());
        assertEquals(0, page.getTotalPages());
    }
}
