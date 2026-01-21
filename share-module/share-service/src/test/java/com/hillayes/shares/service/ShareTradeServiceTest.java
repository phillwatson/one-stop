package com.hillayes.shares.service;

import com.hillayes.exception.common.CommonErrorCodes;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.shares.domain.DealingHistory;
import com.hillayes.shares.domain.Holding;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.errors.SaleExceedsHoldingException;
import com.hillayes.shares.errors.SharesErrorCodes;
import com.hillayes.shares.errors.ZeroTradeQuantityException;
import com.hillayes.shares.event.PortfolioEventSender;
import com.hillayes.shares.repository.HoldingRepository;
import com.hillayes.shares.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ShareTradeServiceTest {
    private final ShareIndexService shareIndexService = mock();
    private final PortfolioRepository portfolioRepository = mock();
    private final HoldingRepository holdingRepository = mock();
    private final PortfolioEventSender portfolioEventSender = mock();

    private final ShareTradeService fixture = new ShareTradeService(
        shareIndexService,
        portfolioRepository,
        holdingRepository,
        portfolioEventSender
    );

    @BeforeEach
    public void beforeEach() {
        // simulate assigning ID to new entities
        when(holdingRepository.saveAndFlush(any())).then(invocation -> {
            Holding entity = invocation.getArgument(0);

            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });
    }

    @Test
    public void testRecordShareTrade_NewHolding() {
        // Given: a share to be traded has been registered
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexService.getShareIndex(shareIndex.getIdentity()))
            .thenReturn(Optional.of(shareIndex));

        // And: an authenticated user
        UUID userId = UUID.randomUUID();

        // And: a portfolio belonging to that user
        Portfolio portfolio = mockPortfolio(userId, p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findByIdOptional(portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // And: portfolio has several holdings but none for this share
        IntStream.range(0, 4).forEach(i -> portfolio.add(mockShareIndex(s -> s.id(UUID.randomUUID()))));
        assertEquals(4, portfolio.getHoldings().size());

        // When: the service is called
        LocalDate dateExecuted = LocalDate.now().minusDays(1);
        int quantity = 1200;
        BigDecimal price = BigDecimal.valueOf(123.435);
        Holding holding = fixture.recordShareTrade(userId, portfolio.getId(),
            dateExecuted, shareIndex.getIdentity(), quantity, price);

        // Then: a new holding is created
        assertNotNull(holding);
        assertEquals(5, portfolio.getHoldings().size());

        // And: the holding was persisted
        verify(holdingRepository).saveAndFlush(any());
        assertNotNull(holding.getId());

        // And: the holding records the new trade
        assertFalse(holding.getDealings().isEmpty());
        DealingHistory dealing = holding.getDealings().get(0);
        assertEquals(dateExecuted, dealing.getDateExecuted());
        assertEquals(quantity, dealing.getQuantity());
        assertEquals(price, dealing.getPrice());

        // And: a notification event was issued
        verify(portfolioEventSender).sendSharesTransacted(dealing);
    }

    @ParameterizedTest
    @ValueSource(ints = { 123, -123 }) // a buy and sell
    public void testRecordShareTrade_ExistingHolding(int quantity) {
        // Given: a share to be traded has been registered
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexService.getShareIndex(shareIndex.getIdentity()))
            .thenReturn(Optional.of(shareIndex));

        // And: an authenticated user
        UUID userId = UUID.randomUUID();

        // And: a portfolio belonging to that user
        Portfolio portfolio = mockPortfolio(userId, p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findByIdOptional(portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // And: portfolio has several holdings including one for this share
        IntStream.range(0, 4).forEach(i -> portfolio.add(mockShareIndex(s -> s.id(UUID.randomUUID()))));
        Holding existingHolding = portfolio.add(shareIndex);
        assertEquals(5, portfolio.getHoldings().size());

        // And: the holding quantity is greater than this sale (to prevent errors)
        if (quantity < 0) {
            existingHolding.buy(
                LocalDate.now().minusDays(100),
                (-quantity) + 100,
                BigDecimal.valueOf(22.22));
        }

        // When: the service is called
        LocalDate dateExecuted = LocalDate.now().minusDays(1);
        BigDecimal price = BigDecimal.valueOf(123.435);
        Holding holding = fixture.recordShareTrade(userId, portfolio.getId(),
            dateExecuted, shareIndex.getIdentity(), quantity, price);

        // Then: the existing holding is returned
        assertEquals(existingHolding, holding);

        // And: no new holding is added to the portfolio
        assertEquals(5, portfolio.getHoldings().size());

        // And: the holding was persisted
        verify(holdingRepository).saveAndFlush(any());
        assertNotNull(holding.getId());

        // And: the holding records the new trade
        assertFalse(holding.getDealings().isEmpty());
        DealingHistory dealing = holding.getDealings().stream()
            .filter(d -> d.getDateExecuted().equals(dateExecuted))
            .findFirst().orElse(null);
        assertNotNull(dealing);
        assertEquals(dateExecuted, dealing.getDateExecuted());
        assertEquals(quantity, dealing.getQuantity());
        assertEquals(price, dealing.getPrice());

        // And: a notification event was issued
        verify(portfolioEventSender).sendSharesTransacted(dealing);
    }

    @Test
    public void testRecordShareTrade_PortfolioNotFound() {
        // Given: a share to be traded has been registered
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexService.getShareIndex(shareIndex.getIdentity()))
            .thenReturn(Optional.of(shareIndex));

        // And: an authenticated user
        UUID userId = UUID.randomUUID();

        // And: NO portfolio belonging to that user with the given ID
        UUID portfolioId = UUID.randomUUID();
        when(portfolioRepository.findByIdOptional(portfolioId))
            .thenReturn(Optional.empty());

        // When: the service is called
        LocalDate dateExecuted = LocalDate.now().minusDays(1);
        int quantity = 201;
        BigDecimal price = BigDecimal.valueOf(123.435);
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.recordShareTrade(userId, portfolioId,
                dateExecuted, shareIndex.getIdentity(), quantity, price));

        // Then: the exception indicates the missing portfolio
        assertEquals(CommonErrorCodes.ENTITY_NOT_FOUND, exception.getErrorCode());
        assertEquals("Portfolio", exception.getParameter("entity-type"));
        assertEquals(portfolioId, exception.getParameter("entity-id"));

        // And: no deal is persisted
        verify(holdingRepository, never()).saveAndFlush(any());

        // And: no notification event was issued
        verifyNoInteractions(portfolioEventSender);
    }

    @Test
    public void testRecordShareTrade_ShareIndexNotFound() {
        // Given: NO share of given ISIN is registered
        ShareIndex.ShareIdentity shareIdentity = mockShareIdentity();
        when(shareIndexService.getShareIndex(shareIdentity))
            .thenReturn(Optional.empty());

        // And: an authenticated user
        UUID userId = UUID.randomUUID();

        // And: a portfolio belonging to that user
        Portfolio portfolio = mockPortfolio(userId, p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findByIdOptional(portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // When: the service is called
        LocalDate dateExecuted = LocalDate.now().minusDays(1);
        int quantity = 201;
        BigDecimal price = BigDecimal.valueOf(123.435);
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.recordShareTrade(userId, portfolio.getId(),
                dateExecuted, shareIdentity, quantity, price));

        // Then: the exception indicates the missing share index
        assertEquals(CommonErrorCodes.ENTITY_NOT_FOUND, exception.getErrorCode());
        assertEquals("ShareIndex", exception.getParameter("entity-type"));
        assertEquals(shareIdentity, exception.getParameter("entity-id"));

        // And: no deal is persisted
        verify(holdingRepository, never()).saveAndFlush(any());

        // And: no notification event was issued
        verifyNoInteractions(portfolioEventSender);
    }

    @Test
    public void testRecordShareTrade_zeroQuantity() {
        // Given: a share to be traded has been registered
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexService.getShareIndex(shareIndex.getIdentity()))
            .thenReturn(Optional.of(shareIndex));

        // And: an authenticated user
        UUID userId = UUID.randomUUID();

        // And: a portfolio belonging to that user
        Portfolio portfolio = mockPortfolio(userId, p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findByIdOptional(portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // And: portfolio has several holdings but none for this share
        IntStream.range(0, 4).forEach(i -> portfolio.add(mockShareIndex(s -> s.id(UUID.randomUUID()))));
        assertEquals(4, portfolio.getHoldings().size());

        // When: the service is called
        LocalDate dateExecuted = LocalDate.now().minusDays(1);
        int quantity = 0;
        BigDecimal price = BigDecimal.valueOf(123.435);
        ZeroTradeQuantityException exception = assertThrows(ZeroTradeQuantityException.class, () ->
            fixture.recordShareTrade(userId, portfolio.getId(),
                dateExecuted, shareIndex.getIdentity(), quantity, price));

        // Then: the exception indicates the ISIN selected
        assertEquals(SharesErrorCodes.ZERO_TRADE_QUANTITY, exception.getErrorCode());
        assertEquals(shareIndex.getIdentity().getIsin(), exception.getParameter("isin"));
        assertEquals(shareIndex.getIdentity().getTickerSymbol(), exception.getParameter("ticker-symbol"));

        // And: no deal is persisted
        verify(holdingRepository, never()).saveAndFlush(any());

        // And: no notification event was issued
        verifyNoInteractions(portfolioEventSender);
    }

    @Test
    public void testSaleExceedsHolding() {
        // Given: a share to be traded has been registered
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexService.getShareIndex(shareIndex.getIdentity()))
            .thenReturn(Optional.of(shareIndex));

        // And: an authenticated user
        UUID userId = UUID.randomUUID();

        // And: a portfolio belonging to that user
        Portfolio portfolio = mockPortfolio(userId, p -> p.id(UUID.randomUUID()));
        when(portfolioRepository.findByIdOptional(portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // And: portfolio has several dealings for this share
        Holding existingHolding = portfolio.add(shareIndex);
        for (int i = 5; i > 0; --i) {
            existingHolding.buy(LocalDate.now().minusDays(i * 10L), 100, BigDecimal.valueOf(123.34));
        }
        assertEquals(5, existingHolding.getDealings().size());
        assertEquals(500, existingHolding.getQuantity());

        // And: dealing is for a sale of more than quantity held
        int quantity = -(10 + existingHolding.getQuantity());

        // When: the service is called
        LocalDate dateExecuted = LocalDate.now().minusDays(1);
        BigDecimal price = BigDecimal.valueOf(123.435);
        SaleExceedsHoldingException exception = assertThrows(SaleExceedsHoldingException.class, () ->
            fixture.recordShareTrade(userId, portfolio.getId(),
                dateExecuted, shareIndex.getIdentity(), quantity, price)
        );

        // Then: the exception indicates the ISIN selected
        assertEquals(SharesErrorCodes.SALE_EXCEEDS_HOLDING, exception.getErrorCode());
        assertEquals(shareIndex.getIdentity().getIsin(), exception.getParameter("isin"));
        assertEquals(shareIndex.getIdentity().getTickerSymbol(), exception.getParameter("ticker-symbol"));
        assertEquals(-quantity, (int)exception.getParameter("quantity"));
        assertEquals(existingHolding.getQuantity(), (int)exception.getParameter("holding"));

        // And: no deal is persisted
        verify(holdingRepository, never()).saveAndFlush(any());

        // And: no notification event was issued
        verifyNoInteractions(portfolioEventSender);
    }
}
