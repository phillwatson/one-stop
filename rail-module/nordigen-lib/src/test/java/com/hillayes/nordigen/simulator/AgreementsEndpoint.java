package com.hillayes.nordigen.simulator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.hillayes.nordigen.model.EndUserAgreement;
import com.hillayes.nordigen.model.EndUserAgreementRequest;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Singleton
@Slf4j
public class AgreementsEndpoint extends AbstractResponseTransformer {
    private final Map<String, EndUserAgreement> agreements = new HashMap<>();

    public AgreementsEndpoint() {
        super(null);
    }

    EndUserAgreement removeAgreement(String id) {
        return agreements.remove(id);
    }

    public void register(WireMockServer wireMockServer) {
        agreements.clear();

        // mock create endpoint
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/agreements/enduser/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(getName())
            )
        );

        // mock list endpoint
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v2/agreements/enduser/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(getName())
                    .withTransformerParameter("list", true)
            )
        );

        // mock get endpoint
        wireMockServer.stubFor(get(urlPathMatching("/api/v2/agreements/enduser/(.*)/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(getName())
            )
        );

        // mock accept endpoint
        wireMockServer.stubFor(put(urlPathMatching("/api/v2/agreements/enduser/(.*)/accept/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(getName())
            )
        );

        // mock delete endpoint
        wireMockServer.stubFor(delete(urlPathMatching("/api/v2/agreements/enduser/(.*)/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(getName())
            )
        );
    }

    @Override
    public ResponseDefinition transform(Request request,
                                        ResponseDefinition responseDefinition,
                                        FileSource files,
                                        Parameters parameters) {
        RequestMethod method = request.getMethod();
        if (method.equals(RequestMethod.POST)) {
            return create(request, responseDefinition);
        }

        if (method.equals(RequestMethod.PUT)) {
            return accept(request, responseDefinition);
        }

        if (method.equals(RequestMethod.GET)) {
            if (parameters.getBoolean("list", false)) {
                return list(request, responseDefinition);
            }
            return getById(request, responseDefinition);
        }

        if (method.equals(RequestMethod.DELETE)) {
            return deleteById(request, responseDefinition);
        }

        return unsupportedMethod(request, responseDefinition);
    }

    private ResponseDefinition create(Request request,
                                      ResponseDefinition responseDefinition) {
        EndUserAgreementRequest agreementRequest = fromJson(request.getBodyAsString(), EndUserAgreementRequest.class);

        return agreements.values().stream()
            .filter(agreement -> agreement.institutionId.equals(agreementRequest.getInstitutionId()))
            .map(agreement -> ResponseDefinitionBuilder.like(responseDefinition)
                .withStatus(409)
                .withBody(toJson(Map.of(
                    "summary", "Conflict",
                    "detail", "An agreement with this Institution already exists.",
                    "status_code", 409
                )))
                .build()
            )
            .findFirst()
            .orElseGet(() ->
                {
                    EndUserAgreement response = new EndUserAgreement();
                    response.id = UUID.randomUUID().toString();
                    response.maxHistoricalDays = agreementRequest.getMaxHistoricalDays();
                    response.accessValidForDays = agreementRequest.getAccessValidForDays();
                    response.accessScope = agreementRequest.getAccessScope();
                    response.institutionId = agreementRequest.getInstitutionId();
                    response.created = OffsetDateTime.now();
                    response.accepted = null;

                    agreements.put(response.id, response);
                    return ResponseDefinitionBuilder.like(responseDefinition)
                        .withStatus(201)
                        .withBody(toJson(response))
                        .build();
                }
            );
    }

    private ResponseDefinition accept(Request request,
                                      ResponseDefinition responseDefinition) {
        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(Map.of(
                "summary", "Insufficient permissions",
                "detail", "Your company doesn't have permission to accept EUA. You'll have to use our default form for this action.",
                "status_code", 403
            )))
            .build();
    }

    private ResponseDefinition list(Request request,
                                    ResponseDefinition responseDefinition) {
        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(sublist(request, agreements.values())))
            .build();
    }

    private ResponseDefinition getById(Request request,
                                       ResponseDefinition responseDefinition) {
        String id = getIdFromPath(request.getUrl(), 5);
        EndUserAgreement entity = agreements.get(id);
        if (entity == null) {
            return notFound(request, responseDefinition);
        }

        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(entity))
            .build();
    }

    private ResponseDefinition deleteById(Request request,
                                          ResponseDefinition responseDefinition) {
        String id = getIdFromPath(request.getUrl(), 5);
        EndUserAgreement entity = removeAgreement(id);

        if (entity == null) {
            return notFound(request, responseDefinition);
        }

        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(Map.of(
                "status_code", 200,
                "summary", "End User Agreement deleted",
                "detail", "End User Agreement " + id + " deleted"
            )))
            .build();
    }
}
