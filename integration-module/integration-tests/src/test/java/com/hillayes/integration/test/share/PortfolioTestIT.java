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
            assertNotNull(portfolio.getHoldings());
            assertTrue(portfolio.getHoldings().isEmpty());

            try (SendInBlueSimulator emailSim = new SendInBlueSimulator(getWiremockPort())) {
                // When: the user creates a trade to buy shares
                TradeRequest buyRequest = new TradeRequest()
                    .shareId(shareIndex.get().getShareId())
                    .dateExecuted(LocalDate.now().minusDays(2))
                    .pricePerShare(123.45)
                    .quantity(100);
                HoldingResponse shareHoldings = portfolioApi.createShareTrade(portfolio.getId(), buyRequest);

                // Then: the trade is recorded
                assertNotNull(shareHoldings);
                assertEquals(buyRequest.getShareId(), shareHoldings.getShareId());
                assertEquals(shareIndex.get().getShareId().getIsin(), shareHoldings.getShareId().getIsin());
                assertEquals(shareIndex.get().getShareId().getTickerSymbol(), shareHoldings.getShareId().getTickerSymbol());
                assertEquals(shareIndex.get().getName(), shareHoldings.getName());
                assertEquals(shareIndex.get().getCurrency(), shareHoldings.getCurrency());

                // And: the holding totals match the single trade
                assertEquals(buyRequest.getQuantity(), shareHoldings.getQuantity());
                assertEquals(buyRequest.getPricePerShare() * 100, shareHoldings.getTotalCost());
                assertNotNull(shareHoldings.getLatestValue());

                // And: the holdings show the new dealing
                assertNotNull(shareHoldings.getDealings());
                assertEquals(1, shareHoldings.getDealings().size());
                DealingHistoryResponse dealing = shareHoldings.getDealings().getFirst();
                assertEquals(buyRequest.getDateExecuted(), dealing.getDateExecuted());
                assertEquals(buyRequest.getQuantity(), dealing.getQuantity());
                assertEquals(buyRequest.getPricePerShare(), dealing.getPricePerShare());

                // And: an email is sent to the user confirming the purchase
                emailSim.verifyEmailSent(user.getEmail(), "Your purchase of shares in " + shareIndex.get().getName());

                // When: the user creates a trade to sell shares
                TradeRequest sellRequest = new TradeRequest()
                    .shareId(shareIndex.get().getShareId())
                    .dateExecuted(LocalDate.now().minusDays(1))
                    .pricePerShare(257.23)
                    .quantity(-10);
                shareHoldings = portfolioApi.createShareTrade(portfolio.getId(), sellRequest);

                // Then: the trade is recorded
                assertNotNull(shareHoldings);
                assertEquals(sellRequest.getShareId(), shareHoldings.getShareId());
                assertEquals(shareIndex.get().getShareId().getIsin(), shareHoldings.getShareId().getIsin());
                assertEquals(shareIndex.get().getShareId().getTickerSymbol(), shareHoldings.getShareId().getTickerSymbol());
                assertEquals(shareIndex.get().getName(), shareHoldings.getName());
                assertEquals(shareIndex.get().getCurrency(), shareHoldings.getCurrency());

                // And: the holding totals match the two trades
                assertEquals(buyRequest.getQuantity() + sellRequest.getQuantity(), shareHoldings.getQuantity());
                assertEquals(
                    (buyRequest.getPricePerShare() * buyRequest.getQuantity()) +
                            (sellRequest.getPricePerShare() * sellRequest.getQuantity()),
                    shareHoldings.getTotalCost());
                assertNotNull(shareHoldings.getLatestValue());

                // And: the holdings show the new dealing
                assertNotNull(shareHoldings.getDealings());
                assertEquals(2, shareHoldings.getDealings().size());
                dealing = shareHoldings.getDealings().get(1);
                assertEquals(sellRequest.getDateExecuted(), dealing.getDateExecuted());
                assertEquals(sellRequest.getQuantity(), dealing.getQuantity());
                assertEquals(sellRequest.getPricePerShare(), dealing.getPricePerShare());

                // And: an email is sent to the user confirming the sale
                emailSim.verifyEmailSent(user.getEmail(), "Your sale of shares in " + shareIndex.get().getName());
            }
        }
    }
}
