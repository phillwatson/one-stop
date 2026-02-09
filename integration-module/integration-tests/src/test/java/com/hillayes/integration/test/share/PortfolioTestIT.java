package com.hillayes.integration.test.share;

import com.hillayes.integration.api.share.PortfolioApi;
import com.hillayes.integration.api.share.ShareIndexApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.util.UserEntity;
import com.hillayes.integration.test.util.UserUtils;
import com.hillayes.onestop.api.*;
import com.hillayes.sim.email.SendInBlueSimulator;
import com.hillayes.sim.ftmarket.FtMarketSimulator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class PortfolioTestIT extends ApiTestBase {
    @Test
    public void testRegisterShareIndex() {
        // given: a user
        UserEntity user = UserUtils.createUser(getWiremockPort(), UserUtils.mockUser());

        ShareIndexApi shareIndexApi = new ShareIndexApi(user.getAuthTokens());
        PortfolioApi portfolioApi = new PortfolioApi(user.getAuthTokens());

        try (FtMarketSimulator ftMarketSimulator = new FtMarketSimulator(getWiremockPort())) {
            // And: a share index is registered
            RegisterShareIndexRequest request = new RegisterShareIndexRequest()
                .isin(randomStrings.nextAlphanumeric(12));

            AtomicReference<ShareIndexResponse> shareIndex = new AtomicReference<>();

            // And: the FT Market data can return the share details
            String ftMarketIssueId = randomStrings.nextNumeric(5);
            ftMarketSimulator.expectSummaryFor(request.getIsin(), ftMarketIssueId, expectSummary ->
                ftMarketSimulator.expectPricesFor(ftMarketIssueId, expectPrices -> {
                    // When: the user registers a share index
                    List<ShareIndexResponse> response = shareIndexApi.registerShareIndices(List.of(request));

                    // Then: a response is returned
                    assertNotNull(response);

                    // And: the FT Market summary was retrieved
                    expectSummary.verify(await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(60)));

                    // And: the FT Market prices were retrieved
                    expectPrices.verify(await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(60)));

                    // record the new share index
                    shareIndex.set(response.getFirst());
                })
            );

            // When: the user creates a portfolio
            PortfolioRequest portfolioRequest = new PortfolioRequest()
                .name(randomStrings.nextAlphanumeric(20));
            PortfolioResponse portfolio = portfolioApi.createPortfolio(portfolioRequest);

            // Then: the named portfolio is created
            assertNotNull(portfolio.getId());
            assertEquals(portfolioRequest.getName(), portfolio.getName());
            assertTrue(portfolio.getDateCreated().isBefore(Instant.now()));

            // And: the portfolio has no holdings
            List<ShareTradeSummaryResponse> holdings = portfolioApi.getPortfolioHoldings(portfolio.getId());
            assertNotNull(holdings);
            assertTrue(holdings.isEmpty());

            try (SendInBlueSimulator emailSim = new SendInBlueSimulator(getWiremockPort())) {
                // When: the user creates a trade to buy shares
                ShareTradeRequest buyRequest = new ShareTradeRequest()
                    .shareIndexId(shareIndex.get().getId())
                    .dateExecuted(LocalDate.now().minusDays(2))
                    .pricePerShare(123.45)
                    .quantity(100.00);
                ShareTradeResponse shareTrade = portfolioApi.createShareTrade(portfolio.getId(), buyRequest);

                // Then: the trade is recorded
                assertNotNull(shareTrade);
                assertEquals(buyRequest.getShareIndexId(), shareTrade.getShareIndexId());
                assertEquals(buyRequest.getQuantity(), shareTrade.getQuantity());
                assertEquals(buyRequest.getPricePerShare(), shareTrade.getPricePerShare());
                assertEquals(buyRequest.getDateExecuted(), shareTrade.getDateExecuted());

                // And: the holdings show the new dealing
                holdings = portfolioApi.getPortfolioHoldings(portfolio.getId());
                assertNotNull(holdings);
                assertEquals(1, holdings.size());
                ShareTradeSummaryResponse newHolding = holdings.getFirst();
                assertEquals(buyRequest.getShareIndexId(), newHolding.getShareIndexId());
                assertEquals(buyRequest.getQuantity().longValue(), newHolding.getQuantity());

                // And: an email is sent to the user confirming the purchase
                emailSim.verifyEmailSent(user.getEmail(), "Your purchase of shares in " + shareIndex.get().getName());

                // When: the user creates a trade to sell shares
                ShareTradeRequest sellRequest = new ShareTradeRequest()
                    .shareIndexId(shareIndex.get().getId())
                    .dateExecuted(LocalDate.now().minusDays(1))
                    .pricePerShare(257.23)
                    .quantity(-10.0);
                shareTrade = portfolioApi.createShareTrade(portfolio.getId(), sellRequest);

                // Then: the trade is recorded
                assertNotNull(shareTrade);
                assertEquals(sellRequest.getShareIndexId(), shareTrade.getShareIndexId());
                assertEquals(sellRequest.getQuantity(), shareTrade.getQuantity());
                assertEquals(sellRequest.getPricePerShare(), shareTrade.getPricePerShare());
                assertEquals(sellRequest.getDateExecuted(), shareTrade.getDateExecuted());

                // And: the holding totals match the two trades
                holdings = portfolioApi.getPortfolioHoldings(portfolio.getId());
                assertNotNull(holdings);
                assertEquals(1, holdings.size());
                ShareTradeSummaryResponse summary = holdings.getFirst();
                assertEquals(buyRequest.getQuantity() + sellRequest.getQuantity(), summary.getQuantity());
                assertEquals(
                    (buyRequest.getPricePerShare() * buyRequest.getQuantity()) +
                            (sellRequest.getPricePerShare() * sellRequest.getQuantity()),
                    summary.getTotalCost());

                // And: an email is sent to the user confirming the sale
                emailSim.verifyEmailSent(user.getEmail(), "Your sale of shares in " + shareIndex.get().getName());
            }
        }
    }
}
