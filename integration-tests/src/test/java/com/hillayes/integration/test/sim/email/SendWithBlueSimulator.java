package com.hillayes.integration.test.sim.email;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionFactory;
import sibModel.CreateSmtpEmail;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
public class SendWithBlueSimulator {
    public static final String BASE_URI = "/api.sendinblue.com/v3";
    private static final String TRANSACTIONAL_EMAIL_URI = BASE_URI + "/smtp/email";

    private final WireMock wireMockClient;

    public SendWithBlueSimulator(int wiremockPort) {
        log.debug("Starting SendWithBlue Simulator [port: {}]", wiremockPort);
        wireMockClient = new WireMock(wiremockPort);

        wireMockClient.register(
            post(TRANSACTIONAL_EMAIL_URI)
                .willReturn(ResponseDefinitionBuilder.okForJson(
                    new CreateSmtpEmail().messageId("<201798300811.5787683@relay.domain.com>")))
        );
    }

    public void stop() {
        wireMockClient.resetRequests();
        wireMockClient.removeMappings();
    }

    public List<LoggedRequest> verifyEmailSent(String emailAddress,
                                               ConditionFactory awaiting) {
        return verifyEmailSent(
            matchingJsonPath("$.to[0].email", equalToIgnoreCase(emailAddress)),
            awaiting);
    }

    public <T> List<LoggedRequest> verifyEmailSent(ContentPattern<T> body,
                                                   ConditionFactory awaiting) {
        RequestPatternBuilder request = postRequestedFor(urlEqualTo(TRANSACTIONAL_EMAIL_URI))
            .withRequestBody(body);

        if (awaiting != null) {
            awaiting.untilAsserted(() -> wireMockClient.verifyThat(request));
        }
        return wireMockClient.find(request);
    }
}
