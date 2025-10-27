package com.hillayes.shares.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.hillayes.shares.utils.TestData.mockPortfolio;
import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.*;

public class PortfolioTest {
    @Test
    public void testAddHolding() {
        // Given: a collection of share indices
        List<ShareIndex> shares = List.of(
            mockShareIndex(s -> s.id(UUID.randomUUID())),
            mockShareIndex(s -> s.id(UUID.randomUUID())),
            mockShareIndex(s -> s.id(UUID.randomUUID()))
        );

        // When: a new portfolio with holdings in each share index
        Portfolio portfolio = mockPortfolio(UUID.randomUUID());
        shares.forEach(portfolio::add);

        // Then: the holdings are assigned to the portfolio
        List<Holding> holdings = portfolio.getHoldings();
        assertNotNull(holdings);
        assertEquals(shares.size(), holdings.size());
        holdings.forEach(holding -> assertEquals(portfolio, holding.getPortfolio()));
    }

    @Test
    public void testRemoveHolding() {
        // Given: a collection of share indices
        List<ShareIndex> shares = List.of(
            mockShareIndex(s -> s.id(UUID.randomUUID())),
            mockShareIndex(s -> s.id(UUID.randomUUID())),
            mockShareIndex(s -> s.id(UUID.randomUUID()))
        );

        // And: a new portfolio with holdings in each share index
        Portfolio portfolio = mockPortfolio(UUID.randomUUID());
        List<Holding> holdings = shares.stream()
            .map(portfolio::add)
            .toList();
        assertEquals(shares.size(), portfolio.getHoldings().size());

        // When: the holdings are removed
        holdings.forEach(holding -> {
            assertTrue(portfolio.remove(holding));

            assertFalse(portfolio.getHoldings().stream()
                .anyMatch(h -> h.equals(holding)));
        });

        // Then: no holdings are left
        assertTrue(portfolio.getHoldings().isEmpty());
    }

    @Test
    public void testRemoveShareIndex() {
        // Given: a collection of share indices
        List<ShareIndex> shares = List.of(
            mockShareIndex(s -> s.id(UUID.randomUUID())),
            mockShareIndex(s -> s.id(UUID.randomUUID())),
            mockShareIndex(s -> s.id(UUID.randomUUID()))
        );

        // And: a new portfolio with holdings in each share index
        Portfolio portfolio = mockPortfolio(UUID.randomUUID());
        shares.forEach(portfolio::add);
        assertEquals(shares.size(), portfolio.getHoldings().size());

        // When: each holding is removed by their share index
        shares.forEach(share -> {
            assertTrue(portfolio.remove(share));

            // Then: the holding is no longer present
            assertNull(portfolio.getHoldings().stream()
                .filter(h -> h.getShareIndex().equals(share))
                .findFirst().orElse(null));
        });

        // And: no holding a left
        assertTrue(portfolio.getHoldings().isEmpty());
    }
}
