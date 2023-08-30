package com.hillayes.integration.test.sim.rail;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.hillayes.rail.model.ObtainJwtResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
public class NordigenSimulator implements Closeable {
    public static final String BASE_URI = "/ob.nordigen.com";

    private final InstitutionsEndpoint institutionsEndpoint;

    private final AgreementsEndpoint agreementsEndpoint;

    private final RequisitionsEndpoint requisitionsEndpoint;

    private final AccountsEndpoint accountsEndpoint;

    private final WireMock wireMockClient;

    public NordigenSimulator(int wiremockPort) {
        log.info("Starting Nordigen Simulator");
        wireMockClient = new WireMock(wiremockPort);
        institutionsEndpoint = new InstitutionsEndpoint();
        agreementsEndpoint = new AgreementsEndpoint();
        accountsEndpoint = new AccountsEndpoint();
        requisitionsEndpoint = new RequisitionsEndpoint(agreementsEndpoint, accountsEndpoint);

        reset();
    }

    /**
     * Stops the wiremock server. Typically called at the end of each test class.
     */
    public void close() {
        wireMockClient.resetRequests();
        wireMockClient.removeMappings();
    }

    /**
     * Resets the simulator to its initial state. Typically called before each test.
     */
    public void reset() {
        wireMockClient.resetRequests();
        wireMockClient.removeMappings();

        // set up minimum stubs
        stubLogin();
        institutionsEndpoint.register(wireMockClient);
        agreementsEndpoint.register(wireMockClient);
        requisitionsEndpoint.register(wireMockClient);
        accountsEndpoint.register(wireMockClient);
    }

    /**
     * Mocks the endpoint to obtain access and refresh tokens from Nordigen.
     */
    private void stubLogin() {
        ObtainJwtResponse response = ObtainJwtResponse.builder()
            .access(UUID.randomUUID().toString())
            .accessExpires(3600)
            .refresh(UUID.randomUUID().toString())
            .refreshExpires(7200)
            .build();

        wireMockClient.register(post(urlEqualTo(NordigenSimulator.BASE_URI + "/api/v2/token/new/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(AbstractResponseTransformer.toJson(response)))
        );
    }

    /**
     * A no-op logger for the WireMock server. Used when logging is not enabled.
     */
    private static class NullNotifier implements Notifier {
        public void info(String message) {}

        public void error(String message) {}

        public void error(String message, Throwable t) {}
    }
}
