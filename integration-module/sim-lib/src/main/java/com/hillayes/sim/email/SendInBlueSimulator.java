package com.hillayes.sim.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hillayes.commons.json.MapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionFactory;
import sibModel.CreateSmtpEmail;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;

@Slf4j
public class SendInBlueSimulator implements Closeable {
    private static final ObjectReader jsonReader = MapperFactory.readerFor(EmailMessage.class);
    private static final ConditionFactory DEFAULT_WAIT =
        await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(60));

    public static final String BASE_URI = "/api.sendinblue.com/v3";
    private static final String TRANSACTIONAL_EMAIL_URI = BASE_URI + "/smtp/email";

    private final WireMock wireMockClient;

    public SendInBlueSimulator(int wiremockPort) {
        log.info("Starting SendWithBlue Simulator [port: {}]", wiremockPort);
        wireMockClient = new WireMock(wiremockPort);

        wireMockClient.register(
            post(TRANSACTIONAL_EMAIL_URI)
                .willReturn(ResponseDefinitionBuilder.okForJson(
                    new CreateSmtpEmail().messageId("<201798300811.5787683@relay.domain.com>")))
        );
    }

    public void close() {
        wireMockClient.resetRequests();
        wireMockClient.removeMappings();
    }

    /**
     * Returns the email content of the given wiremock request.
     * @param request the request returned from wiremock email server interception
     * @return the content of the email extracted from the given request.
     */
    public EmailMessage parse(LoggedRequest request) {
        try {
            return jsonReader.readValue(request.getBodyAsString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LoggedRequest> verifyEmailSent(String toEmailAddress) {
        return verifyEmailSent(toEmailAddress, DEFAULT_WAIT);
    }

    public List<LoggedRequest> verifyEmailSent(String toEmailAddress,
                                               ConditionFactory awaiting) {
        log.info("Verifying email sent [address: {}]", toEmailAddress);
        return verifyEmailSent(
            matchingJsonPath("$.to[0].email", equalToIgnoreCase(toEmailAddress)),
            awaiting);
    }

    public List<LoggedRequest> verifyEmailSent(String toEmailAddress,
                                               String subject) {
        return verifyEmailSent(toEmailAddress, subject, DEFAULT_WAIT);
    }

    public List<LoggedRequest> verifyEmailSent(String toEmailAddress,
                                               String subject,
                                               ConditionFactory awaiting) {
        log.info("Verifying email sent [address: {}, subject: {}]", toEmailAddress, subject);
        return verifyEmailSent(
            matchingJsonPath("$.to[0].email", equalToIgnoreCase(toEmailAddress))
                .and(matchingJsonPath("$.subject", containing(subject))),
            awaiting);
    }

    public <T> List<LoggedRequest> verifyEmailSent(ContentPattern<T> body) {
        return verifyEmailSent(body, DEFAULT_WAIT);
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
