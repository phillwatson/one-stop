package com.hillayes.shares.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.hillayes.shares.utils.TestData.mockPortfolio;
import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HoldingTest {
    @Test
    public void testBuy() {
        // Given: A portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), "Share ISA");

        // And: a share index
        ShareIndex index = mockShareIndex();

        // And: a holding within that portfolio
        Holding holding = portfolio.add(index);

        // When: a purchase is recorded
        DealingHistory purchase = holding.buy(LocalDate.now(), 100, BigDecimal.valueOf(123.92));

        // Then: the holdings dealings is increased
        assertEquals(1, holding.getDealings().size());

        // And: the purchase reflects the price
        assertNotNull(purchase);
        assertEquals(100, purchase.getQuantity());
        assertEquals(BigDecimal.valueOf(123.92), purchase.getPrice());
    }

    @Test
    public void testBuyNegativeQuantity() {
        // Given: A portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), "Share ISA");

        // And: a share index
        ShareIndex index = mockShareIndex();

        // And: a holding within that portfolio
        Holding holding = portfolio.add(index);

        // When: a purchase is recorded
        DealingHistory purchase = holding.buy(LocalDate.now(), -100, BigDecimal.valueOf(123.92));

        // Then: the holdings dealings is increased
        assertEquals(1, holding.getDealings().size());

        // And: the purchase reflects the price - with a positive quantity
        assertNotNull(purchase);
        assertEquals(100, purchase.getQuantity());
        assertEquals(BigDecimal.valueOf(123.92), purchase.getPrice());
    }

    @Test
    public void testSell() {
        // Given: A portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), "Share ISA");

        // And: a share index
        ShareIndex index = mockShareIndex();

        // And: a holding within that portfolio
        Holding holding = portfolio.add(index);

        // When: a sale is recorded
        DealingHistory sale = holding.sell(LocalDate.now(), 100, BigDecimal.valueOf(123.92));

        // Then: the holdings dealings is increased
        assertEquals(1, holding.getDealings().size());

        // And: the sale reflects the price - with a negative quantity
        assertNotNull(sale);
        assertEquals(-100, sale.getQuantity());
        assertEquals(BigDecimal.valueOf(123.92), sale.getPrice());
    }

    @Test
    public void testSellNegativeQuantity() {
        // Given: A portfolio
        Portfolio portfolio = mockPortfolio(UUID.randomUUID(), "Share ISA");

        // And: a share index
        ShareIndex index = mockShareIndex();

        // And: a holding within that portfolio
        Holding holding = portfolio.add(index);

        // When: a sale is recorded
        DealingHistory sale = holding.sell(LocalDate.now(), -100, BigDecimal.valueOf(123.92));

        // Then: the holdings dealings is increased
        assertEquals(1, holding.getDealings().size());

        // And: the sale reflects the price - with a negative quantity
        assertNotNull(sale);
        assertEquals(-100, sale.getQuantity());
        assertEquals(BigDecimal.valueOf(123.92), sale.getPrice());
    }
}
