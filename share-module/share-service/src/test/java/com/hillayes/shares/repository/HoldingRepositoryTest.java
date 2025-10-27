package com.hillayes.shares.repository;

import com.hillayes.shares.domain.Holding;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.mockPortfolio;
import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
@RequiredArgsConstructor
public class HoldingRepositoryTest {
    private final ShareIndexRepository shareIndexRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;

    @Test
    public void testGetHolding() {
        // Given: several share indices
        List<ShareIndex> shares = shareIndexRepository.saveAll(
            IntStream.range(0, 10).mapToObj(i -> mockShareIndex() ).toList()
        );

        // And: a portfolio to which the holdings belong
        Portfolio portfolio = portfolioRepository.save(mockPortfolio(UUID.randomUUID()));

        // And: a holding within that portfolio for each share index
        shares.forEach(portfolio::add);

        // And: the portfolio and holdings are persisted
        portfolioRepository.saveAndFlush(portfolio);
        portfolioRepository.getEntityManager().clear();

        shares.forEach(shareIndex -> {
            // When: we retrieve the holdings for each share index
            Optional<Holding> holding = holdingRepository.getHolding(portfolio.getId(), shareIndex.getId());

            // Then: the holding is returned
            assertNotNull(holding);
            assertTrue(holding.isPresent());
            assertEquals(portfolio.getId(), holding.get().getPortfolio().getId());
            assertEquals(shareIndex.getId(), holding.get().getShareIndex().getId());

            // And: the portfolio can be navigated to from the holding
            assertEquals(portfolio, holding.get().getPortfolio());
        });
    }

}
