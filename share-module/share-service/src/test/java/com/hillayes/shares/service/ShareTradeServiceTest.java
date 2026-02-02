package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.*;
import com.hillayes.shares.errors.ZeroTradeQuantityException;
import com.hillayes.shares.event.PortfolioEventSender;
import com.hillayes.shares.repository.PortfolioRepository;
import com.hillayes.shares.repository.PriceHistoryRepository;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.repository.ShareTradeRepository;
import groovy.lang.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ShareTradeServiceTest {
    private final ShareTradeRepository shareTradeRepository = mock();
    private final ShareIndexRepository shareIndexRepository = mock();
    private final PriceHistoryRepository priceHistoryRepository = mock();
    private final PortfolioRepository portfolioRepository = mock();
    private final PortfolioEventSender portfolioEventSender = mock();

    private final ShareTradeService shareTradeService = new ShareTradeService(
        shareTradeRepository, shareIndexRepository, priceHistoryRepository,
        portfolioRepository, portfolioEventSender
    );

    @BeforeEach
    public void beforeEach() {
        when(shareTradeRepository.saveAndFlush(any(ShareTrade.class))).then(invocation -> {
            ShareTrade shareTrade = invocation.getArgument(0);
            if (shareTrade.getId() == null) {
                shareTrade.setId(UUID.randomUUID());
            }
            return shareTrade;
        });

    }

    @Test
    public void testGetShareTrade() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: the share trade is requested
        Optional<ShareTrade> result = shareTradeService
            .getShareTrade(portfolio.getUserId(), shareTrade.getId());

        // Then: the repository is called
        verify(shareTradeRepository).findByIdOptional(shareTrade.getId());

        // And: the identified trade is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertSame(shareTrade, result.get());
    }

    @Test
    public void testGetShareTrade_WrongUser() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: the share trade is requested by another user
        UUID invalidUserId = UUID.randomUUID();
        Optional<ShareTrade> result = shareTradeService
            .getShareTrade(invalidUserId, shareTrade.getId());

        // Then: the repository is called
        verify(shareTradeRepository).findByIdOptional(shareTrade.getId());

        // And: the identified trade is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetShareTrade_NotFound() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: another share trade is requested
        UUID invalidShareTradeId = UUID.randomUUID();
        Optional<ShareTrade> result = shareTradeService
            .getShareTrade(portfolio.getUserId(), invalidShareTradeId);

        // Then: the repository is called
        verify(shareTradeRepository).findByIdOptional(invalidShareTradeId);

        // And: an empty result returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRecordShareTrade() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // When: a trade is recorded
        LocalDate dateExecuted = LocalDate.now().minusDays(10);
        int quantity = 100;
        BigDecimal pricePerShare = BigDecimal.TEN;
        ShareTrade shareTrade = shareTradeService
            .recordShareTrade(portfolio, shareIndex, dateExecuted, quantity, pricePerShare);

        // Then: the repository saves the new request
        verify(shareTradeRepository).saveAndFlush(any());

        // And: the new trade is returned
        assertNotNull(shareTrade);
        assertEquals(portfolio.getUserId(), shareTrade.getUserId());
        assertEquals(portfolio.getId(), shareTrade.getPortfolioId());
        assertEquals(shareIndex.getId(), shareTrade.getShareIndexId());
        assertEquals(dateExecuted, shareTrade.getDateExecuted());
        assertEquals(quantity, shareTrade.getQuantity());
        assertEquals(pricePerShare, shareTrade.getPrice());

        // And: an event is issued to notify of the trade
        verify(portfolioEventSender).sendSharesTransacted(portfolio, shareIndex, shareTrade);
    }

    @Test
    public void testRecordShareTrade_ZeroQuantity() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // When: a trade is recorded
        LocalDate dateExecuted = LocalDate.now().minusDays(10);
        int quantity = 0;
        BigDecimal pricePerShare = BigDecimal.TEN;
        ZeroTradeQuantityException exception = assertThrows(ZeroTradeQuantityException.class, () ->
            shareTradeService.recordShareTrade(portfolio, shareIndex, dateExecuted, quantity, pricePerShare));

        // Then: the repository is not called
        verifyNoInteractions(shareTradeRepository);

        // And: an exception is raised
        assertNotNull(exception);
        assertEquals(shareIndex.getIdentity().getIsin(), exception.getParameter("isin"));
        assertEquals(shareIndex.getIdentity().getTickerSymbol(), exception.getParameter("ticker-symbol"));

        // And: NO event is issued to notify of the trade
        verifyNoInteractions(portfolioEventSender);
    }

    @Test
    public void testUpdateShareTrade() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: the share trade is updated
        LocalDate dateExecuted = LocalDate.now().minusDays(10);
        int quantity = 100;
        BigDecimal pricePerShare = BigDecimal.TEN;
        Optional<ShareTrade> result = shareTradeService
            .updateShareTrade(portfolio.getUserId(), shareTrade.getId(), dateExecuted, quantity, pricePerShare);

        // Then: the repository is called to retrieve the share trade
        verify(shareTradeRepository).findByIdOptional(shareTrade.getId());

        // And: the repository is called to save the modified trade
        ArgumentCaptor<ShareTrade> shareTradeCaptor = ArgumentCaptor.forClass(ShareTrade.class);
        verify(shareTradeRepository).saveAndFlush(shareTradeCaptor.capture());

        ShareTrade update = shareTradeCaptor.getValue();
        assertEquals(shareTrade.getId(), update.getId());
        assertEquals(portfolio.getUserId(), update.getUserId());
        assertEquals(portfolio.getId(), update.getPortfolioId());
        assertEquals(shareIndex.getId(), update.getShareIndexId());
        assertEquals(dateExecuted, update.getDateExecuted());
        assertEquals(quantity, update.getQuantity());
        assertEquals(pricePerShare, update.getPrice());

        // And: the updated trade is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertSame(update, result.get());

        // And: an event is issued to notify of the trade update
        verify(portfolioEventSender).sendShareTradeUpdated(portfolio, shareIndex, update);
    }

    @Test
    public void testUpdateShareTrade_WrongUser() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: the share trade is updated by another user
        LocalDate dateExecuted = LocalDate.now().minusDays(10);
        int quantity = 100;
        BigDecimal pricePerShare = BigDecimal.TEN;
        UUID invalidUserId = UUID.randomUUID();
        Optional<ShareTrade> result = shareTradeService
            .updateShareTrade(invalidUserId, shareTrade.getId(), dateExecuted, quantity, pricePerShare);

        // Then: the repository is called to retrieve the share trade
        verify(shareTradeRepository).findByIdOptional(shareTrade.getId());

        // And: the repository is NOT called to save the modified trade
        verify(shareTradeRepository, never()).saveAndFlush(any());

        // And: an empty result is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // And: NO event is issued to notify of the trade update
        verifyNoInteractions(portfolioEventSender);
    }

    @Test
    public void testUpdateShareTrade_NotFound() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: the share trade is updated by another user
        LocalDate dateExecuted = LocalDate.now().minusDays(10);
        int quantity = 100;
        BigDecimal pricePerShare = BigDecimal.TEN;
        UUID invalidTradeId = UUID.randomUUID();
        Optional<ShareTrade> result = shareTradeService
            .updateShareTrade(portfolio.getUserId(), invalidTradeId, dateExecuted, quantity, pricePerShare);

        // Then: the repository is called to retrieve the share trade
        verify(shareTradeRepository).findByIdOptional(invalidTradeId);

        // And: the repository is NOT called to save the modified trade
        verify(shareTradeRepository, never()).saveAndFlush(any());

        // And: an empty result is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // And: NO event is issued to notify of the trade update
        verifyNoInteractions(portfolioEventSender);
    }

    @Test
    public void testUpdateShareTrade_ZeroQuantity() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: the share trade is updated
        LocalDate dateExecuted = LocalDate.now().minusDays(10);
        int quantity = 0;
        BigDecimal pricePerShare = BigDecimal.TEN;
        ZeroTradeQuantityException exception = assertThrows(ZeroTradeQuantityException.class, () ->
            shareTradeService.updateShareTrade(portfolio.getUserId(), shareTrade.getId(), dateExecuted, quantity, pricePerShare));

        // Then: the repository is called to retrieve the share trade
        verify(shareTradeRepository).findByIdOptional(shareTrade.getId());

        // And: an exception is raised
        assertNotNull(exception);
        assertEquals(shareIndex.getIdentity().getIsin(), exception.getParameter("isin"));
        assertEquals(shareIndex.getIdentity().getTickerSymbol(), exception.getParameter("ticker-symbol"));

        // And: the repository is NOT called to save the modified trade
        verify(shareTradeRepository, never()).saveAndFlush(any());

        // And: NO event is issued to notify of the trade update
        verifyNoInteractions(portfolioEventSender);
    }

    @Test
    public void testDeleteShareTrade() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: the share trade is deleted
        Optional<ShareTrade> result = shareTradeService
            .deleteShareTrade(portfolio.getUserId(), shareTrade.getId());

        // Then: the repository is called to retrieve the trade
        verify(shareTradeRepository).findByIdOptional(shareTrade.getId());

        // And: the repository is called to delete the trade
        verify(shareTradeRepository).delete(shareTrade);

        // And: the identified trade is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertSame(shareTrade, result.get());

        // And: an event is issued to notify of the trade's deletion
        verify(portfolioEventSender).sendShareTradeDeleted(portfolio, shareIndex, shareTrade);
    }

    @Test
    public void testDeleteShareTrade_WrongUser() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: the share trade is deleted by another user
        UUID invalidUserId = UUID.randomUUID();
        Optional<ShareTrade> result = shareTradeService
            .deleteShareTrade(invalidUserId, shareTrade.getId());

        // Then: the repository is called to retrieve the trade
        verify(shareTradeRepository).findByIdOptional(shareTrade.getId());

        // And: the repository is NOT called to delete the trade
        verify(shareTradeRepository, never()).delete(any());

        // And: an empty result is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // And: NO event is issued to notify of the trade's deletion
        verifyNoInteractions(portfolioEventSender);
    }

    @Test
    public void testDeleteShareTrade_NotFound() {
        // Given: a share index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexRepository.findById(shareIndex.getId())).thenReturn(shareIndex);

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findById(portfolio.getId())).thenReturn(portfolio);

        // And: a share trade exists
        ShareTrade shareTrade = mockShareTrade(portfolio, shareIndex, t -> t.id(UUID.randomUUID()));
        when(shareTradeRepository.findByIdOptional(shareTrade.getId()))
            .thenReturn(Optional.of(shareTrade));

        // When: am unknown share trade is deleted by another user
        UUID invalidShareTradeId = UUID.randomUUID();
        Optional<ShareTrade> result = shareTradeService
            .deleteShareTrade(portfolio.getUserId(), invalidShareTradeId);

        // Then: the repository is called to retrieve the trade
        verify(shareTradeRepository).findByIdOptional(invalidShareTradeId);

        // And: the repository is NOT called to delete the trade
        verify(shareTradeRepository, never()).delete(any());

        // And: an empty result is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // And: NO event is issued to notify of the trade's deletion
        verifyNoInteractions(portfolioEventSender);
    }

    @Test
    public void testGetShareTradeSummaries() {
        // Given: a collection of share indices
        List<ShareIndex> indices = IntStream.range(0, 3)
            .mapToObj(i -> mockShareIndex(s -> s.id(UUID.randomUUID())))
            .toList();

        // And: a set of latest prices - keyed on share index id
        Map<UUID, PriceHistory> prices = indices.stream()
            .map(index -> mockPriceHistory(index, LocalDate.now().minusDays(1)))
            .collect(Collectors.toMap(price -> price.getId().getShareIndexId(), p -> p));
        when(priceHistoryRepository.getMostRecent(any(UUID.class))).then(invocation ->
            Optional.ofNullable(prices.get(invocation.getArgument(0, UUID.class))));

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));

        // And: a collection of share trade summaries exist
        Map<UUID, ShareTradeRepository.ShareTradeSummaryProjection> summaries = indices.stream()
            .map(index -> mockTradeSummary(portfolio, index))
            .collect(Collectors.toMap(ShareTradeRepository.ShareTradeSummaryProjection::getShareIndexId, s -> s));
        when(shareTradeRepository.getShareTradeSummaries(portfolio.getUserId(), portfolio.getId()))
            .thenReturn(summaries.values().stream().toList());

        // When: the summaries are requested
        List<ShareTradeSummary> result = shareTradeService.getShareTradeSummaries(portfolio);

        // Then: the repository is called to get the trade summaries
        verify(shareTradeRepository).getShareTradeSummaries(portfolio.getUserId(), portfolio.getId());

        // And: the price repository is called to retrieve each share index price
        indices.forEach(index -> verify(priceHistoryRepository).getMostRecent(index.getId()));

        // And: the result is expected size
        assertNotNull(result);
        assertEquals(indices.size(), result.size());

        // And: each share index is accounted for
        result.forEach(summary -> {
            ShareTradeRepository.ShareTradeSummaryProjection expected = summaries.get(summary.getShareIndexId());
            assertNotNull(expected);

            assertEquals(expected.getPortfolioId(), summary.getPortfolioId());
            assertEquals(expected.getIsin(), summary.getShareIdentity().getIsin());
            assertEquals(expected.getTickerSymbol(), summary.getShareIdentity().getTickerSymbol());
            assertEquals(expected.getName(), summary.getName());
            assertEquals(expected.getCurrency(), summary.getCurrency().getCurrencyCode());
            assertEquals(expected.getQuantity(), summary.getQuantity());
            assertEquals(expected.getTotalCost(), summary.getTotalCost());

            PriceHistory priceHistory = prices.get(summary.getShareIndexId());
            assertNotNull(priceHistory);
            assertEquals(priceHistory.getClose(), summary.getLatestPrice());
        });
    }

    @Test
    public void testGetShareTradeSummaries_NoShareTrades() {
        // Given: a collection of share indices
        List<ShareIndex> indices = IntStream.range(0, 3)
            .mapToObj(i -> mockShareIndex(s -> s.id(UUID.randomUUID())))
            .toList();

        // And: a set of latest prices - keyed on share index id
        Map<UUID, PriceHistory> prices = indices.stream()
            .map(index -> mockPriceHistory(index, LocalDate.now().minusDays(1)))
            .collect(Collectors.toMap(price -> price.getId().getShareIndexId(), p -> p));
        when(priceHistoryRepository.getMostRecent(any(UUID.class))).then(invocation ->
            Optional.ofNullable(prices.get(invocation.getArgument(0, UUID.class))));

        // And: a user's portfolio - with no share trades
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));

        // And: NO share trades exist
        when(shareTradeRepository.getShareTradeSummaries(portfolio.getUserId(), portfolio.getId()))
            .thenReturn(List.of());

        // When: the summaries are requested
        List<ShareTradeSummary> result = shareTradeService.getShareTradeSummaries(portfolio);

        // Then: the repository is called to get the trade summaries
        verify(shareTradeRepository).getShareTradeSummaries(portfolio.getUserId(), portfolio.getId());

        // And: the price repository is NOT called
        indices.forEach(index -> verify(priceHistoryRepository, never()).getMostRecent(index.getId()));

        // And: the result is empty
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetShareTrades() {
        // Given: a share index
        ShareIndex index = mockShareIndex(s -> s.id(UUID.randomUUID()));

        // And: a user's portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), p -> p.id(UUID.randomUUID()));

        // When: the share index trades are requested
        shareTradeService.getShareTrades(portfolio, index, 0, 10);

        // Then: the repository is called to retrieve the selected page
        verify(shareTradeRepository).getShareTrades(portfolio, index, 0, 10);
    }

    private ShareTradeRepository.ShareTradeSummaryProjection mockTradeSummary(Portfolio portfolio,
                                                                              ShareIndex shareIndex) {
        return ShareTradeRepository.ShareTradeSummaryProjection.builder()
            .portfolioId(portfolio.getId())
            .shareIndexId(shareIndex.getId())
            .isin(shareIndex.getIdentity().getIsin())
            .tickerSymbol(shareIndex.getIdentity().getTickerSymbol())
            .name(shareIndex.getName())
            .quantity(randomNumbers.randomInt(1, 200))
            .totalCost(BigDecimal.valueOf(randomNumbers.randomDouble(100, 1000)))
            .currency(shareIndex.getCurrency().getCurrencyCode())
            .build();
    }
}
