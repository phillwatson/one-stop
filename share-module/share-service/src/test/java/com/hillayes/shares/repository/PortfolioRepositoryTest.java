package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.Portfolio;
import io.quarkus.panache.common.Parameters;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static com.hillayes.shares.utils.TestData.mockPortfolio;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestTransaction
@RequiredArgsConstructor
public class PortfolioRepositoryTest {
    private final ShareIndexRepository shareIndexRepository;
    private final PortfolioRepository portfolioRepository;

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
                    portfolioRepository.save(mockPortfolio(userId))
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
        userIds.forEach(userId ->
            IntStream.range(0, portfoliosPerUser).forEach(index ->
                portfolioRepository.save(mockPortfolio(userId))
            )
        );

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
