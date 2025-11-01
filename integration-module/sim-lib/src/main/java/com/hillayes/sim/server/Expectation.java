package com.hillayes.sim.server;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionFactory;

import java.io.Closeable;
import java.util.List;

@Slf4j
public class Expectation implements Closeable {
    private final String name;
    private final WireMock wireMockClient;
    private final StubMapping stubMapping;
    private final RequestPatternBuilder request;

    public Expectation(String name,
                       WireMock wireMockClient,
                       StubMapping stubMapping,
                       RequestPatternBuilder request) {
        this.name = name;
        this.wireMockClient = wireMockClient;
        this.stubMapping = stubMapping;
        this.request = request;
        wireMockClient.register(stubMapping);
    }

    public <T> List<LoggedRequest> verify(ConditionFactory awaiting) {
        log.info("Verifying {}", name);
        if (awaiting != null) {
            awaiting.untilAsserted(() -> wireMockClient.verifyThat(request));
        }

        return wireMockClient.find(request);
    }


    @Override
    public void close() {
        wireMockClient.removeStubMapping(stubMapping);
    }
}
