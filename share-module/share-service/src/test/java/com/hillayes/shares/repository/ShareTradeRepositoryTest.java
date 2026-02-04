package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.ShareTrade;
import com.hillayes.shares.repository.ShareTradeRepository.ShareTradeSummaryProjection;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static com.hillayes.shares.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
@RequiredArgsConstructor
public class ShareTradeRepositoryTest {
    private final ShareIndexRepository shareIndexRepository;
    private final PortfolioRepository portfolioRepository;
    private final ShareTradeRepository shareTradeRepository;

    @Test
    public void testShareTradeSummary() {
        // Given: a collection of user IDs
        List<UUID> userIds = IntStream.range(0, 5)
            .mapToObj(index -> UUID.randomUUID()).toList();

        // And: a collection of shares
        List<ShareIndex> indices = IntStream.range(0, 5)
            .mapToObj(index -> mockShareIndex()).toList();
        shareIndexRepository.saveAll(indices);

        // And: two portfolios with share trades for each user ID
        long portfoliosPerUser = 2;
        Map<UUID, List<Portfolio>> portfolios = userIds.stream()
            .flatMap(userId ->
                LongStream.range(0, portfoliosPerUser).mapToObj(n -> {
                    Portfolio portfolio = portfolioRepository.save(mockPortfolio(userId));

                    indices.forEach(shareIndex -> {
                        for (int i = 1; i < 3; i++) {
                            int index = i;
                            shareTradeRepository.save(mockShareTrade(portfolio, shareIndex, t ->
                                t.quantity(10 * index).price(BigDecimal.valueOf(100 * index))
                            ));
                        }
                    });
                    return portfolio;
                })
            )
            .collect(Collectors.groupingBy(Portfolio::getUserId));

        shareTradeRepository.flush();

        // When: the portfolios' trade summary are requested
        portfolios.forEach((userId, portfolioList) ->
            portfolioList.forEach(portfolio -> {
                List<ShareTradeSummaryProjection> summaries = shareTradeRepository.getShareTradeSummaries(userId, portfolio.getId());

                assertNotNull(summaries);
                assertEquals(5, summaries.size());
                summaries.forEach(summary -> {
                    ShareIndex shareIndex = indices.stream()
                        .filter(s -> s.getId().equals(summary.getShareIndexId()))
                        .findAny()
                        .orElse(null);

                    assertNotNull(shareIndex);
                    assertEquals(portfolio.getId(), summary.getPortfolioId());
                    assertEquals(shareIndex.getIdentity().getIsin(), summary.getIsin());
                    assertEquals(shareIndex.getIdentity().getTickerSymbol(), summary.getTickerSymbol());
                    assertEquals(shareIndex.getName(), summary.getName());
                    assertEquals(shareIndex.getCurrency().getCurrencyCode(), summary.getCurrency());
                    assertEquals(30, summary.getQuantity());
                    assertEquals(5000.0, summary.getTotalCost().doubleValue());
                });
            })
        );
    }

    @Test
    public void testShareTradeSummary_NonExist() {
        // Given: a collection of shares
        List<ShareIndex> indices = List.of(
            shareIndexRepository.save(mockShareIndex()),
            shareIndexRepository.save(mockShareIndex())
        );

        // And: a user's share portfolio
        UUID userId = UUID.randomUUID();
        Portfolio portfolio = portfolioRepository.save(mockPortfolio(userId));

        // When: the portfolio's trade summary is requested
        List<ShareTradeSummaryProjection> summaries = shareTradeRepository.getShareTradeSummaries(userId, portfolio.getId());

        // Then: an empty list is returned
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    public void testGetShareTrades() {
        // Given: a collection of shares
        List<ShareIndex> indices = List.of(
            shareIndexRepository.save(mockShareIndex()),
            shareIndexRepository.save(mockShareIndex())
        );

        // And: a user's share portfolio
        UUID userId = UUID.randomUUID();
        Portfolio portfolio = portfolioRepository.save(mockPortfolio(userId));

        // And: the portfolio has several trades for each share
        indices.forEach(shareIndex -> {
            LocalDate date = LocalDate.now().minusDays(30);
            LocalDate now = LocalDate.now();
            while (date.isBefore(now)) {
                LocalDate dateExecuted = date;
                shareTradeRepository.save(
                    mockShareTrade(portfolio, shareIndex, t -> t.dateExecuted(dateExecuted))
                );
                date = date.plusDays(1);
            }
        });

        // When: the trades are requested for a particular share
        indices.forEach(shareIndex -> {
            for (int pageIndex = 0; pageIndex < 3; pageIndex++) {
                Page<ShareTrade> page = shareTradeRepository
                    .getShareTrades(portfolio, shareIndex, pageIndex, 10);

                // Then: a page is returned
                assertNotNull(page);

                // And: the page contains the expected results
                assertEquals(pageIndex, page.getPageIndex());
                assertEquals(10, page.getPageSize());
                assertEquals(3, page.getTotalPages());
                assertEquals(30, page.getTotalCount());
                assertEquals(10, page.getContentSize());

                // And: the page items are for the requested share and in ascending date order
                LocalDate prevDate = null;
                for (ShareTrade trade : page.getContent()) {
                    assertEquals(portfolio.getId(), trade.getPortfolioId());
                    assertEquals(shareIndex.getId(), trade.getShareIndexId());

                    if (prevDate == null) {
                        prevDate = trade.getDateExecuted();
                    } else {
                        assertTrue(prevDate.isBefore(trade.getDateExecuted()));
                    }
                }
            }
        });
    }

    @Test
    public void testGetShareTrades_NonExist() {
        // Given: a collection of shares
        List<ShareIndex> indices = List.of(
            shareIndexRepository.save(mockShareIndex()),
            shareIndexRepository.save(mockShareIndex())
        );

        // And: a user's share portfolio with trades only on the first share
        UUID userId = UUID.randomUUID();
        Portfolio portfolio = portfolioRepository.save(mockPortfolio(userId));

        // And: the portfolio has several trades for each share
        ShareIndex shareIndex = indices.getFirst();
        LocalDate date = LocalDate.now().minusDays(30);
        LocalDate now = LocalDate.now();
        while (date.isBefore(now)) {
            LocalDate dateExecuted = date;
            shareTradeRepository.save(
                mockShareTrade(portfolio, shareIndex, t -> t.dateExecuted(dateExecuted))
            );
            date = date.plusDays(1);
        }

        // When: the trades are requested for another share
        Page<ShareTrade> page = shareTradeRepository
            .getShareTrades(portfolio, indices.get(1), 0, 10);

        // Then: a page is returned
        assertNotNull(page);

        // And: the page is empty
        assertEquals(0, page.getPageIndex());
        assertEquals(10, page.getPageSize());
        assertEquals(0, page.getTotalPages());
        assertEquals(0, page.getTotalCount());
        assertEquals(0, page.getContentSize());
    }

    @Test
    public void testGetShareTrades_EmptyPage() {
        // Given: a share
        ShareIndex shareIndex = shareIndexRepository.save(mockShareIndex());

        // And: a user's share portfolio with trades only on the first share
        UUID userId = UUID.randomUUID();
        Portfolio portfolio = portfolioRepository.save(mockPortfolio(userId));

        // And: the portfolio has several trades for the share
        LocalDate date = LocalDate.now().minusDays(30);
        LocalDate now = LocalDate.now();
        while (date.isBefore(now)) {
            LocalDate dateExecuted = date;
            shareTradeRepository.save(
                mockShareTrade(portfolio, shareIndex, t -> t.dateExecuted(dateExecuted))
            );
            date = date.plusDays(1);
        }

        // When: the trades are requested for a page beyond number of trades
        Page<ShareTrade> page = shareTradeRepository
            .getShareTrades(portfolio, shareIndex, 4, 10);

        // Then: a page is returned
        assertNotNull(page);

        // And: the page is empty
        assertEquals(4, page.getPageIndex());
        assertEquals(10, page.getPageSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(30, page.getTotalCount());
        assertEquals(0, page.getContentSize());
    }
}
