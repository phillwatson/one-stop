package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.PriceHistory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceResolution;
import com.hillayes.shares.errors.DatabaseException;
import com.hillayes.shares.utils.TestData;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.hillayes.shares.utils.TestData.mockPriceHistory;
import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestTransaction
public class PriceHistoryRepositoryTest {
    @Inject
    private ShareIndexRepository shareIndexRepository;
    @InjectSpy
    private PriceHistoryRepository priceHistoryRepository;
    @InjectSpy
    private PriceHistorySqlMapper sqlMapper;

    @BeforeEach
    public void beforeEach() {
        // wait until any batches are completed
        Awaitility.await()
            .atMost(Duration.ofSeconds(3))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> !priceHistoryRepository.isBatchPending());

        shareIndexRepository.deleteAll();
    }

    @AfterEach
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void deleteShareIndices() {
        shareIndexRepository.deleteAll();
    }

    @Test
    public void testListPrices() {
        // Given: a share index
        ShareIndex shareIndex = shareIndexRepository.save(mockShareIndex());

        // And: the index has a collection of daily prices
        LocalDate startDate = LocalDate.now().minusDays(50);
        LocalDate today = LocalDate.now();
        LocalDate date = startDate;
        List<PriceHistory> prices = new ArrayList<>();
        while (date.isBefore(today)) {
            prices.add(TestData.mockPriceHistory(shareIndex, date, SharePriceResolution.DAILY));
            date = date.plusDays(1);
        }
        priceHistoryRepository.saveAll(prices);

        // When:
        LocalDate fromDate = LocalDate.now().minusDays(40);
        LocalDate toDate = LocalDate.now().minusDays(2);
        Page<PriceHistory> page = priceHistoryRepository
            .listPrices(shareIndex, SharePriceResolution.DAILY, fromDate, toDate, 1, 20);

        // Then:
        assertNotNull(page);
        assertFalse(page.isEmpty());
        assertEquals(38, page.getTotalCount());
        assertEquals(2, page.getTotalPages());
        assertEquals(18, page.getContentSize());
        assertEquals(1, page.getPageIndex());
        assertEquals(20, page.getPageSize());
    }

    @Test
    public void testSaveBatch_Empty() {
        // Given: the initial number of prices
        long originalCount = priceHistoryRepository.count();

        // And: an empty collection of share prices to be saved
        List<PriceHistory> prices = List.of();

        // When: the batch is saved
        priceHistoryRepository.saveBatch(prices);

        // And: batches are completed
        Awaitility.await()
            .atMost(Duration.ofSeconds(3))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> !priceHistoryRepository.isBatchPending());

        // Then: no new prices are persisted and no exceptions raised
        assertEquals(originalCount, priceHistoryRepository.count());
    }

    @Test
    public void testSaveBatch_LargeBatch() {
        // Given: a share index
        ShareIndex shareIndex = createShareIndex(mockShareIndex());

        // And: a collection of share prices to be saved
        int batchSize = (priceHistoryRepository.getInsertBatchSize() * 5)
            + (priceHistoryRepository.getInsertBatchSize() / 2);
        List<PriceHistory> prices = IntStream.range(0, batchSize).mapToObj(index -> {
            LocalDate marketDate = LocalDate.now().minusDays(index);
            return mockPriceHistory(shareIndex, marketDate, SharePriceResolution.DAILY);
        }).toList();

        // When: the batch is saved
        priceHistoryRepository.saveBatch(prices);

        // And: batches are completed
        Awaitility.await()
            .atMost(Duration.ofSeconds(3))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> !priceHistoryRepository.isBatchPending());

        // Then: all prices are persisted
        assertEquals(batchSize, priceHistoryRepository.count("id.shareIndexId", shareIndex.getId()));
    }

    @Test
    public void testSaveBatch_SmallBatch() {
        // Given: a share index
        ShareIndex shareIndex = createShareIndex(mockShareIndex());

        // And: a collection of share prices to be saved
        int batchSize = (priceHistoryRepository.getInsertBatchSize() - 5);
        List<PriceHistory> prices = IntStream.range(0, batchSize).mapToObj(index -> {
            LocalDate marketDate = LocalDate.now().minusDays(index);
            return mockPriceHistory(shareIndex, marketDate, SharePriceResolution.DAILY);
        }).toList();

        // When: the batch is saved
        priceHistoryRepository.saveBatch(prices);

        // And: batches are completed
        Awaitility.await()
            .atMost(Duration.ofSeconds(3))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> !priceHistoryRepository.isBatchPending());

        // Then: all prices are persisted
        assertEquals(batchSize, priceHistoryRepository.count("id.shareIndexId", shareIndex.getId()));
    }

    @Test
    public void testSaveBatch_WithDuplicates() {
        // Given: a share index
        ShareIndex shareIndex = createShareIndex(mockShareIndex());

        List<PriceHistory> prices = Stream.concat(
            // And: a collection of 8 share prices to be saved
            IntStream.range(0, 8).mapToObj(index -> {
            LocalDate marketDate = LocalDate.now().minusDays(index);
            return mockPriceHistory(shareIndex, marketDate, SharePriceResolution.DAILY);
        }),
            // And: two with duplicates keys from those above
            Stream.of(
                mockPriceHistory(shareIndex, LocalDate.now().minusDays(2), SharePriceResolution.DAILY),
                mockPriceHistory(shareIndex, LocalDate.now().minusDays(3), SharePriceResolution.DAILY)
            )
        ).toList();

        // When: the batch is saved
        priceHistoryRepository.saveBatch(prices);

        // And: batches are completed
        Awaitility.await()
            .atMost(Duration.ofSeconds(3))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> !priceHistoryRepository.isBatchPending());

        // Then: only non-duplicates prices are persisted
        assertEquals(8, priceHistoryRepository.count("id.shareIndexId", shareIndex.getId()));
    }

    @Test
    public void testSaveBatch_WithMapperException() {
        // Given: a share index
        ShareIndex shareIndex = createShareIndex(mockShareIndex());

        // And: a collection of share prices to be saved
        int batchSize = (priceHistoryRepository.getInsertBatchSize() * 5)
            + (priceHistoryRepository.getInsertBatchSize() / 2);
        List<PriceHistory> prices = IntStream.range(0, batchSize).mapToObj(index -> {
            LocalDate marketDate = LocalDate.now().minusDays(index);
            return mockPriceHistory(shareIndex, marketDate, SharePriceResolution.DAILY);
        }).toList();

        // And: a batch will fail
        PriceHistory failedRecord = prices.get(prices.size() / 2);
        doThrow(new DatabaseException(new SQLException("mock failure")))
            .when(sqlMapper).map(any(), anyInt(), eq(failedRecord));

        // When: the batch is saved
        priceHistoryRepository.saveBatch(prices);

        // And: batches are completed
        Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .until(() -> !priceHistoryRepository.isBatchPending());

        // Then: the batch is saved in smaller batches
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<PriceHistory>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(priceHistoryRepository, atLeast(6))._saveBatch(captor.capture());

        // Then: all BUT the batch with failed price are persisted
        int failedBatchSize = captor.getAllValues().stream()
            .filter(batch -> batch.contains(failedRecord))
            .findFirst()
            .or(() -> fail("Unable to identity failed batch"))
            .map(Collection::size)
            .orElse(0);
        assertEquals(batchSize - failedBatchSize,
            priceHistoryRepository.count("id.shareIndexId", shareIndex.getId()));
    }

    @Test
    public void testGetMostRecent() {
        // Given: a share index
        ShareIndex shareIndex = shareIndexRepository.save(mockShareIndex());

        // And: the index has a collection of daily prices
        LocalDate startDate = LocalDate.now().minusDays(50);
        LocalDate today = LocalDate.now();
        LocalDate date = startDate;
        List<PriceHistory> prices = new ArrayList<>();
        while (date.isBefore(today)) {
            prices.add(TestData.mockPriceHistory(shareIndex, date, SharePriceResolution.DAILY));
            date = date.plusDays(1);
        }
        priceHistoryRepository.saveAll(prices);
        priceHistoryRepository.flush();

        // When: the most recent price is requested
        Optional<PriceHistory> mostRecent = priceHistoryRepository.getMostRecent(shareIndex);

        // Then: a price is returned
        assertNotNull(mostRecent);
        assertTrue(mostRecent.isPresent());

        // And: the correct price is returned
        PriceHistory expected = prices.getLast();
        assertEquals(expected.getId(), mostRecent.get().getId());
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public ShareIndex createShareIndex(ShareIndex shareIndex) {
        return shareIndexRepository.save(shareIndex);
    }
}
