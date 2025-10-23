package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.Holding;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import io.quarkus.panache.common.Parameters;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static com.hillayes.shares.utils.TestData.mockPortfolio;
import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
@RequiredArgsConstructor
public class PortfolioRepositoryTest {
    private final ShareIndexRepository shareIndexRepository;
    private final PortfolioRepository portfolioRepository;

    @Test
    public void testSaveOneToMany() {
        // Given: a collection of share indices
        Iterable<ShareIndex> shares = shareIndexRepository.saveAll(List.of(
            mockShareIndex(),
            mockShareIndex(),
            mockShareIndex()
        ));

        // And: the initial number of portfolios
        long portfolioCount = getPortfolioCount();

        // And: a new portfolio with holdings in each share index
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), "share ISA");
        shares.forEach(portfolio::add);
        assertFalse(portfolio.getHoldings().isEmpty());

        // When: the portfolio is saved
        portfolioRepository.save(portfolio);

        // Then: an ID is assigned to the portfolio
        assertNotNull(portfolio.getId());

        // And: the portfolio count is incremented
        assertEquals(portfolioCount + 1, getPortfolioCount());

        // And: the portfolio's holdings are saved
        assertEquals(portfolio.getHoldings().size(), getHoldingCount(portfolio));

        // And: IDs are assigned to each holding
        Set<Holding> holdings = portfolio.getHoldings();
        assertNotNull(holdings);
        assertEquals(3, holdings.size());
        holdings.forEach(holding -> assertNotNull(holding.getId()));
    }

    @Test
    public void testDeleteOneToMany() {
        // Given: a collection of share indices
        Iterable<ShareIndex> shares = shareIndexRepository.saveAll(List.of(
            mockShareIndex(),
            mockShareIndex(),
            mockShareIndex()
        ));

        // And: the initial number of portfolios
        long originalCount = getPortfolioCount();

        // And: a new portfolio with holdings in each share index
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), "share ISA");
        shares.forEach(portfolio::add);
        assertFalse(portfolio.getHoldings().isEmpty());

        // When: the portfolio is saved
        portfolio = portfolioRepository.save(portfolio);

        // Then: the portfolio count is incremented
        assertEquals(originalCount + 1, getPortfolioCount());

        // And: the portfolio's holdings are saved
        assertEquals(portfolio.getHoldings().size(), getHoldingCount(portfolio));

        // When: the portfolio is deleted
        portfolioRepository.delete(portfolio);

        // Then: the portfolio is deleted
        assertEquals(originalCount, getPortfolioCount());

        // And: the holdings are deleted
        assertEquals(0, getHoldingCount(portfolio));
    }

    @Test
    public void testDeleteUsersPortfolios() {
        // Given: a collection of user IDs
        List<UUID> userIds = IntStream.range(0, 5)
            .mapToObj(index -> UUID.randomUUID()).toList();

        // And: the initial number of portfolios
        long originalCount = getPortfolioCount();

        // And: two portfolios for each user ID
        long portfoliosPerUser = 2;
        Map<UUID, List<Portfolio>> portfolios = userIds.stream()
            .flatMap(userId ->
                LongStream.range(0, portfoliosPerUser).mapToObj(index ->
                    portfolioRepository.save(mockPortfolio(userId, "Portfolio: " + index))
                )
            )
            .collect(Collectors.groupingBy(Portfolio::getUserId));

        AtomicLong expectedCount = new AtomicLong(originalCount + (portfoliosPerUser * portfolios.size()));
        assertEquals(expectedCount.get(), getPortfolioCount());

        // When: a user's portfolios are deleted
        userIds.forEach(userId -> {
            portfolioRepository.deleteUsersPortfolios(userId);

            // Then: only their portfolios are deleted
            assertEquals(expectedCount.addAndGet(-portfoliosPerUser), getPortfolioCount());
        });

        // And: all portfolios were deleted
        assertEquals(originalCount, getPortfolioCount());
    }

    @Test
    public void testGetUsersPortfolios() {
        // Given: a collection of user IDs
        List<UUID> userIds = IntStream.range(0, 5)
            .mapToObj(index -> UUID.randomUUID()).toList();

        // And: two portfolios for each user ID
        int portfoliosPerUser = 2;
        Map<UUID, List<Portfolio>> portfolios = userIds.stream()
            .flatMap(userId ->
                LongStream.range(0, portfoliosPerUser).mapToObj(index ->
                    portfolioRepository.save(mockPortfolio(userId, "Portfolio: " + index))
                )
            )
            .collect(Collectors.groupingBy(Portfolio::getUserId));

        // When: a user's portfolios are retrieved
        int pageSize = portfoliosPerUser + 5; // plus more for good measure
        userIds.forEach(userId -> {
            Page<Portfolio> usersPortfolios =
                portfolioRepository.getUsersPortfolios(userId, 0, pageSize);

            // Then: only their portfolios are retrieved
            assertEquals(portfoliosPerUser, usersPortfolios.getTotalCount());
            assertEquals(portfoliosPerUser, usersPortfolios.getContentSize());
            usersPortfolios.getContent().forEach(portfolio ->
                assertEquals(userId, portfolio.getUserId())
            );
        });
    }

    private long getPortfolioCount() {
        return portfolioRepository.count();
    }

    private long getHoldingCount(Portfolio portfolio) {
        return portfolioRepository
            .count("from Holding h where h.portfolio = :portfolio",
                Parameters.with("portfolio", portfolio));
    }
}
