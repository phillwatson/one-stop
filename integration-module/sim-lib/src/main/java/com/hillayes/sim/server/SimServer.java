package com.hillayes.sim.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@ApplicationScoped
@Slf4j
public class SimServer {
    public static final int WIREMOCK_PORT = 8089;

    @Inject
    @Any
    Instance<SimExtension> extensions;

    private WireMockServer wireMockServer;

    @Startup
    public void init() {
        log.info("Starting SimServer");
        SimExtension[] array = extensions.stream().toArray(SimExtension[]::new);
        log.info("Starting SimServer [extensions: {}]", array.length);
        wireMockServer = new WireMockServer(
            options()
                .port(WIREMOCK_PORT)
                .notifier(new ConsoleNotifier(true))
                .extensions(array)
        );

        wireMockServer.start();
    }
}
