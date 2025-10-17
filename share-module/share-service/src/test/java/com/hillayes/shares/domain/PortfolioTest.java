package com.hillayes.shares.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.hillayes.shares.utils.TestData.mockPortfolio;
import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.*;

public class PortfolioTest {
    @Test
    public void testAddHolding() {
        // Given: a collection of share indexes
        List<ShareIndex> shares = List.of(
            mockShareIndex(),
            mockShareIndex(),
            mockShareIndex()
        );

        // When: a new portfolio with holdings in each share index
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), "share ISA");
        shares.forEach(portfolio::add);

        // Then: the holdings are assigned to the portfolio
        Set<Holding> holdings = portfolio.getHoldings();
        assertNotNull(holdings);
        assertEquals(shares.size(), holdings.size());
        holdings.forEach(holding -> assertEquals(portfolio, holding.getPortfolio()));
    }

    @Test
    public void testRemoveHolding() {
        // Given: a collection of share indexes
        List<ShareIndex> shares = List.of(
            mockShareIndex(),
            mockShareIndex(),
            mockShareIndex()
        );

        // And: a new portfolio with holdings in each share index
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), "share ISA");
        List<Holding> holdings = shares.stream()
            .map(portfolio::add)
            .toList();
        assertEquals(shares.size(), holdings.size());

        // When: the holdings are removed
        holdings.forEach(holding -> {
            assertTrue(portfolio.remove(holding));
        });

        // Then: no holdings are left
        assertTrue(portfolio.getHoldings().isEmpty());
    }

    @Test
    public void testRemoveShareIndex() {
        // Given: a collection of share indexes
        List<ShareIndex> shares = List.of(
            mockShareIndex(),
            mockShareIndex(),
            mockShareIndex()
        );

        // And: a new portfolio with holdings in each share index
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), "share ISA");
        List<Holding> holdings = shares.stream()
            .map(portfolio::add)
            .toList();
        assertEquals(shares.size(), holdings.size());

        // When: the holdings are removed by their share index
        shares.forEach(share -> {
            assertTrue(portfolio.remove(share));
        });

        // Then: no holdings are left
        assertTrue(portfolio.getHoldings().isEmpty());
    }
}
