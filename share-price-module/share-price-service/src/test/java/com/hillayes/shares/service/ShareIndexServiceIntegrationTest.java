package com.hillayes.shares.service;

import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceHistory;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.repository.SharePriceHistoryRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@RequiredArgsConstructor
public class ShareIndexServiceIntegrationTest {
    private final ShareIndexRepository shareIndexRepository;
    private final SharePriceHistoryRepository sharePriceHistoryRepository;
    private final ShareIndexService shareIndexService;

    @BeforeEach
    @Transactional
    public void beforeEach() {
        shareIndexRepository.deleteAll();
    }

    @Test
    public void testRefreshSharePrices() {
        // Given: a share index
        ShareIndex shareIndex = createShareIndex();

        // When: the prices are refreshed
        int newEntryCount = shareIndexService.refreshSharePrices(shareIndex.getId());

        // Then: prices are retrieved
        assertTrue(newEntryCount > 0);

        // And: the batches of prices are processed
        Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> !sharePriceHistoryRepository.isBatchPending());

        // And: the prices are persisted
        List<SharePriceHistory> allPrices = getAllPrices();
        assertEquals(newEntryCount, allPrices.size());
    }

    @Transactional
    public ShareIndex createShareIndex() {
        ShareIndex shareIndex = ShareIndex.builder()
            .provider(ShareProvider.FT_MARKET_DATA)
            .isin("GB00B80QG052")
            .name("HSBC FTSE 250 Index Accumulation C")
            .currency(Currency.getInstance("GBP"))
            .build();
        shareIndexRepository.save(shareIndex);

        return shareIndex;
    }

    @Transactional
    public List<SharePriceHistory> getAllPrices() {
        return sharePriceHistoryRepository.findAll().list();
    }
}
