package com.hillayes.nordigen.simulator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.hillayes.nordigen.model.ObtainJwtResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@Singleton
@Slf4j
public class NordigenSimulator {
    @Inject
    InstitutionsEndpoint institutionsEndpoint;

    @Inject
    AgreementsEndpoint agreementsEndpoint;

    @Inject
    RequisitionsEndpoint requisitionsEndpoint;

    @Inject
    AccountsEndpoint accountsEndpoint;

    @ConfigProperty(name = "one-stop.tests.nordigen.port")
    int portNumber;

    private WireMockServer wireMockServer;

    @PostConstruct
    public void init() {
        log.debug("Starting Nordigen Simulator");
        wireMockServer = new WireMockServer(
            options()
                .port(portNumber)
                .notifier(new ConsoleNotifier(true))
                .extensions(
                    institutionsEndpoint,
                    agreementsEndpoint,
                    requisitionsEndpoint,
                    accountsEndpoint
                )
        );

        wireMockServer.start();
    }

    /**
     * Stops the wiremock server. Typically called at the end of each test class.
     */
    @PreDestroy
    public void stop() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    /**
     * Resets the simulator to its initial state. Typically called before each test.
     */
    public void reset() {
        wireMockServer.resetAll();

        // set up minimum stubs
        stubLogin();
        institutionsEndpoint.register(wireMockServer);
        agreementsEndpoint.register(wireMockServer);
        requisitionsEndpoint.register(wireMockServer);
        accountsEndpoint.register(wireMockServer);
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

        wireMockServer.stubFor(post(urlEqualTo("/api/v2/token/new/"))
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
