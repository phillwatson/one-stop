package com.hillayes.shares.service;

import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.config.ShareProviderFactory;
import com.hillayes.shares.domain.PriceHistory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceResolution;
import com.hillayes.shares.repository.PriceHistoryRepository;
import com.hillayes.shares.repository.ShareIndexRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class ShareIndexServiceTest {
    private final ShareIndexRepository shareIndexRepository = mock();
    private final PriceHistoryRepository priceHistoryRepository = mock();
    private final ShareProviderFactory providerFactory = mock();

    private final ShareIndexService fixture = new ShareIndexService(
        shareIndexRepository,
        priceHistoryRepository,
        providerFactory
    );

    @Test
    public void testRefreshSharePrices_Success() {
        // Given: a Share Index to be refreshed
        ShareIndex shareIndex = mockShareIndex(b -> b.id(UUID.randomUUID()));
        when(shareIndexRepository.findByIdOptional(shareIndex.getId()))
            .thenReturn(Optional.of(shareIndex));

        // And: the price records are out-of-date
        PriceHistory mostRecent = mockPriceHistory(shareIndex, LocalDate.now().minusDays(10), SharePriceResolution.DAILY);
        when(priceHistoryRepository.getMostRecent(shareIndex))
            .thenReturn(Optional.of(mostRecent));

        // And: the share provider is available
        ShareProviderApi shareProviderApi = mock();
        when(providerFactory.get(shareIndex.getProvider()))
            .thenReturn(shareProviderApi);

        // And: the provider has prices available
        AtomicReference<LocalDate> marketDate = new AtomicReference<>(LocalDate.now().minusDays(10));
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<PriceData> prices = IntStream.range(0, 10).mapToObj(index -> {
            PriceData priceData = mockPriceData(marketDate.get());
            marketDate.set(marketDate.get().plusDays(1));
            return priceData;
        }).toList();
        when(shareProviderApi.getPrices(shareIndex.getIsin(), mostRecent.getId().getDate(), yesterday))
            .thenReturn(Optional.of(prices));

        // When: the service is called
        int recordCount = fixture.refreshSharePrices(shareIndex.getId());

        // Then: the provider is called to retrieve the share prices over the given dates
        verify(shareProviderApi).getPrices(shareIndex.getIsin(), mostRecent.getId().getDate(), yesterday);

        // And: the repository is called to save the prices as a batch
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PriceHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(priceHistoryRepository).saveBatch(captor.capture());

        // And: each record reflects the data retrieved from the provider
        captor.getValue().forEach(priceHistory -> {
            PriceData expected = prices.stream().filter(p -> p.date() == priceHistory.getId().getDate())
                .findFirst().orElse(null);
            assertNotNull(expected);

            assertEquals(shareIndex.getId(), priceHistory.getId().getShareIndexId());
            assertEquals(SharePriceResolution.DAILY, priceHistory.getId().getResolution());
            assertEquals(expected.date(), priceHistory.getId().getDate());
            assertEquals(expected.open(), priceHistory.getOpen());
            assertEquals(expected.high(), priceHistory.getHigh());
            assertEquals(expected.low(), priceHistory.getLow());
            assertEquals(expected.close(), priceHistory.getClose());
        });

        // And: the result count equals the share prices retrieved
        assertEquals(prices.size(), recordCount);
    }

    @Test
    public void testRefreshSharePrices_NoPriorHistory() {
        // Given: a Share Index to be refreshed
        ShareIndex shareIndex = mockShareIndex(b -> b.id(UUID.randomUUID()));
        when(shareIndexRepository.findByIdOptional(shareIndex.getId()))
            .thenReturn(Optional.of(shareIndex));

        // And: NO historic prices currently exist
        when(priceHistoryRepository.getMostRecent(shareIndex))
            .thenReturn(Optional.empty());

        // And: the share provider is available
        ShareProviderApi shareProviderApi = mock();
        int providerMaxHistory = 90;
        when(shareProviderApi.getMaxHistory()).thenReturn(providerMaxHistory);
        when(providerFactory.get(shareIndex.getProvider()))
            .thenReturn(shareProviderApi);

        // And: the provider has prices available
        AtomicReference<LocalDate> marketDate = new AtomicReference<>(LocalDate.now().minusDays(providerMaxHistory));
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<PriceData> prices = IntStream.range(0, providerMaxHistory).mapToObj(index -> {
            PriceData priceData = mockPriceData(marketDate.get());
            marketDate.set(marketDate.get().plusDays(1));
            return priceData;
        }).toList();
        when(shareProviderApi.getPrices(eq(shareIndex.getIsin()), any(), eq(yesterday)))
            .thenReturn(Optional.of(prices));

        // When: the service is called
        int recordCount = fixture.refreshSharePrices(shareIndex.getId());

        // Then: the provider is called to retrieve the share prices over the given dates
        ArgumentCaptor<LocalDate> fromDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(shareProviderApi).getPrices(eq(shareIndex.getIsin()), fromDateCaptor.capture(), eq(yesterday));

        // And: the from-date is calculated from the provider API
        verify(shareProviderApi).getMaxHistory();
        LocalDate expectedFromDate = LocalDate.now().minusDays(providerMaxHistory);
        assertEquals(expectedFromDate, fromDateCaptor.getValue());

        // And: the repository is called to save the prices as a batch
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PriceHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(priceHistoryRepository).saveBatch(captor.capture());

        // And: each record reflects the data retrieved from the provider
        captor.getValue().forEach(priceHistory -> {
            PriceData expected = prices.stream().filter(p -> p.date() == priceHistory.getId().getDate())
                .findFirst().orElse(null);
            assertNotNull(expected);

            assertEquals(shareIndex.getId(), priceHistory.getId().getShareIndexId());
            assertEquals(SharePriceResolution.DAILY, priceHistory.getId().getResolution());
            assertEquals(expected.date(), priceHistory.getId().getDate());
            assertEquals(expected.open(), priceHistory.getOpen());
            assertEquals(expected.high(), priceHistory.getHigh());
            assertEquals(expected.low(), priceHistory.getLow());
            assertEquals(expected.close(), priceHistory.getClose());
        });

        // And: the result count equals the share prices retrieved
        assertEquals(prices.size(), recordCount);
    }

    @Test
    public void testRefreshSharePrices_PricesAreUpToDate() {
        // Given: a Share Index to be refreshed
        ShareIndex shareIndex = mockShareIndex(b -> b.id(UUID.randomUUID()));
        when(shareIndexRepository.findByIdOptional(shareIndex.getId()))
            .thenReturn(Optional.of(shareIndex));

        // And: the price records are up-to-date
        PriceHistory mostRecent = mockPriceHistory(shareIndex, LocalDate.now().minusDays(1), SharePriceResolution.DAILY);
        when(priceHistoryRepository.getMostRecent(shareIndex))
            .thenReturn(Optional.of(mostRecent));

        // And: the share provider is available
        ShareProviderApi shareProviderApi = mock();
        when(providerFactory.get(shareIndex.getProvider()))
            .thenReturn(shareProviderApi);

        // When: the service is called
        int recordCount = fixture.refreshSharePrices(shareIndex.getId());

        // And: the most recent share price is retrieved
        verify(priceHistoryRepository).getMostRecent(any());

        // And: the provider is never called
        verify(shareProviderApi, never()).getPrices(any(), any(), any());

        // And: no records are saved
        verify(priceHistoryRepository, never()).saveBatch(any(List.class));

        // And: the result is zero
        assertEquals(0, recordCount);
    }

    @Test
    public void testRefreshSharePrices_ShareIndexNotFound() {
        // Given: the Share Index to be refreshed cannot be found
        ShareIndex shareIndex = mockShareIndex(b -> b.id(UUID.randomUUID()));
        when(shareIndexRepository.findByIdOptional(shareIndex.getId()))
            .thenReturn(Optional.empty());

        // When: the service is called
        int recordCount = fixture.refreshSharePrices(shareIndex.getId());

        // Then: the provider is never called
        verifyNoInteractions(providerFactory);

        // And: no share prices are retrieved
        verify(priceHistoryRepository, never()).getMostRecent(any());

        // And: no records are saved
        verifyNoInteractions(priceHistoryRepository);

        // And: the result is zero
        assertEquals(0, recordCount);
    }

    @Test
    public void testRefreshSharePrices_ProviderUnableTogetSharePrices() {
        // Given: a Share Index to be refreshed
        ShareIndex shareIndex = mockShareIndex(b -> b.id(UUID.randomUUID()));
        when(shareIndexRepository.findByIdOptional(shareIndex.getId()))
            .thenReturn(Optional.of(shareIndex));

        // And: the price records are out-of-date
        PriceHistory mostRecent = mockPriceHistory(shareIndex, LocalDate.now().minusDays(10), SharePriceResolution.DAILY);
        when(priceHistoryRepository.getMostRecent(shareIndex))
            .thenReturn(Optional.of(mostRecent));

        // And: the share provider is available
        ShareProviderApi shareProviderApi = mock();
        when(providerFactory.get(shareIndex.getProvider()))
            .thenReturn(shareProviderApi);

        // And: the provider is unable to find share index
        when(shareProviderApi.getPrices(eq(shareIndex.getIsin()), any(), any()))
            .thenReturn(Optional.empty());

        // When: the service is called
        int recordCount = fixture.refreshSharePrices(shareIndex.getId());

        // Then: the provider is called to retrieve the share prices over the given dates
        LocalDate yesterday = LocalDate.now().minusDays(1);
        verify(shareProviderApi).getPrices(shareIndex.getIsin(), mostRecent.getId().getDate(), yesterday);

        // And: no records are saved
        verify(priceHistoryRepository, never()).saveBatch(any(Collection.class));

        // And: the result is zero
        assertEquals(0, recordCount);
    }

    @Test
    public void testRefreshSharePrices_ProviderHasNoPrices() {
        // Given: a Share Index to be refreshed
        ShareIndex shareIndex = mockShareIndex(b -> b.id(UUID.randomUUID()));
        when(shareIndexRepository.findByIdOptional(shareIndex.getId()))
            .thenReturn(Optional.of(shareIndex));

        // And: the price records are out-of-date
        PriceHistory mostRecent = mockPriceHistory(shareIndex, LocalDate.now().minusDays(10), SharePriceResolution.DAILY);
        when(priceHistoryRepository.getMostRecent(shareIndex))
            .thenReturn(Optional.of(mostRecent));

        // And: the share provider is available
        ShareProviderApi shareProviderApi = mock();
        when(providerFactory.get(shareIndex.getProvider()))
            .thenReturn(shareProviderApi);

        // And: the provider is able to find share index BUT has none
        when(shareProviderApi.getPrices(eq(shareIndex.getIsin()), any(), any()))
            .thenReturn(Optional.of(List.of()));

        // When: the service is called
        int recordCount = fixture.refreshSharePrices(shareIndex.getId());

        // Then: the provider is called to retrieve the share prices over the given dates
        LocalDate yesterday = LocalDate.now().minusDays(1);
        verify(shareProviderApi).getPrices(shareIndex.getIsin(), mostRecent.getId().getDate(), yesterday);

        // And: no records are saved
        verify(priceHistoryRepository, never()).saveBatch(any(Collection.class));

        // And: the result is zero
        assertEquals(0, recordCount);
    }
}
