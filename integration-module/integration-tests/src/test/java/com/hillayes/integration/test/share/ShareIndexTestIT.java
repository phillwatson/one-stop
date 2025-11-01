package com.hillayes.integration.test.share;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hillayes.integration.api.share.ShareIndexApi;
import com.hillayes.integration.test.ApiTestBase;
import com.hillayes.integration.test.util.UserEntity;
import com.hillayes.integration.test.util.UserUtils;
import com.hillayes.onestop.api.RegisterShareIndexRequest;
import com.hillayes.onestop.api.ShareIndexResponse;
import com.hillayes.sim.ftmarket.FtMarketSimulator;
import com.hillayes.sim.server.Expectation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ShareIndexTestIT extends ApiTestBase {
    @Test
    public void testRegisterShareIndex() {
        // given: a user
        UserEntity user = UserUtils.createUser(getWiremockPort(), UserEntity.builder()
            .username(randomStrings.nextAlphanumeric(20))
            .givenName(randomStrings.nextAlphanumeric(10))
            .password(randomStrings.nextAlphanumeric(30))
            .email(randomStrings.nextAlphanumeric(30))
            .build());

        // And:
        try (FtMarketSimulator ftMarketSimulator = new FtMarketSimulator(getWiremockPort())) {
            RegisterShareIndexRequest request = new RegisterShareIndexRequest()
                .provider("FT_MARKET_DATA")
                .isin(randomStrings.nextAlphanumeric(12))
                .name(randomStrings.nextAlphanumeric(30))
                .currency("GBP");

            // And: the FT Market data can return the share details
            String ftMarketIssueId = randomStrings.nextNumeric(5);
            try (Expectation expectSummary = ftMarketSimulator
                .expectSummaryFor(request.getIsin(), ftMarketIssueId, request.getName(), request.getCurrency())) {

                try (Expectation expectPrices = ftMarketSimulator.expectPricesFor(ftMarketIssueId)) {

                    // When: the user registers a share index
                    ShareIndexApi shareIndexApi = new ShareIndexApi(user.getAuthTokens());
                    List<ShareIndexResponse> response = shareIndexApi.registerShareIndices(List.of(request));

                    // Then: a response is returned
                    assertNotNull(response);

                    // And: the FT Market summary was retrieved
                    expectSummary.verify(await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(60)));

                    // And: the FT Market prices were retrieved
                    expectPrices.verify(await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(60)));
                }
            }
        }
    }
}
