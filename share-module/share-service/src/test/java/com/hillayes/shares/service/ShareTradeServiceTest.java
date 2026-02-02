package com.hillayes.shares.service;

import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.ShareTrade;
import com.hillayes.shares.errors.ZeroTradeQuantityException;
import com.hillayes.shares.event.PortfolioEventSender;
import com.hillayes.shares.repository.PortfolioRepository;
import com.hillayes.shares.repository.PriceHistoryRepository;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.repository.ShareTradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

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
        fail("Not yet implemented");
    }

    @Test
    public void testGetShareTrades() {
        fail("Not yet implemented");
    }
}
