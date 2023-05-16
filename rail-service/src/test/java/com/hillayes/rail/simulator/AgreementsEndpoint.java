package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.hillayes.rail.model.EndUserAgreement;
import com.hillayes.rail.model.EndUserAgreementRequest;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.hillayes.rail.utils.TestData.fromJson;
import static com.hillayes.rail.utils.TestData.toJson;

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
                aResponse().withTransformers(AgreementsEndpoint.class.getSimpleName())
            )
        );

        // mock list endpoint
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v2/agreements/enduser/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withTransformers(AgreementsEndpoint.class.getSimpleName())
                    .withTransformerParameter("list", true)
            )
        );

        // mock get endpoint
        wireMockServer.stubFor(get(urlPathMatching("/api/v2/agreements/enduser/(.*)/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse().withTransformers(AgreementsEndpoint.class.getSimpleName())
            )
        );

        // mock accept endpoint
        wireMockServer.stubFor(put(urlPathMatching("/api/v2/agreements/enduser/(.*)/accept/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse().withTransformers(AgreementsEndpoint.class.getSimpleName())
            )
        );

        // mock delete endpoint
        wireMockServer.stubFor(delete(urlPathMatching("/api/v2/agreements/enduser/(.*)/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse().withTransformers(AgreementsEndpoint.class.getSimpleName())
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
            return create(request);
        }

        if (method.equals(RequestMethod.PUT)) {
            return accept(request);
        }

        if (method.equals(RequestMethod.GET)) {
            if (parameters.getBoolean("list", false)) {
                return list(request);
            }
            return getById(request);
        }

        if (method.equals(RequestMethod.DELETE)) {
            return deleteById(request);
        }

        return new ResponseDefinitionBuilder()
            .withStatus(400)
            .withHeader("Content-Type", "application/json")
            .withBody(toJson(Map.of(
                "status_code", 400,
                "summary", "Unsupported method",
                "detail", "This endpoint does not support the " + method + " method."
            )))
            .build();
    }

    private ResponseDefinition create(Request request) {
        EndUserAgreementRequest agreementRequest = fromJson(request.getBodyAsString(), EndUserAgreementRequest.class);

        return agreements.values().stream()
            .filter(agreement -> agreement.institutionId.equals(agreementRequest.getInstitutionId()))
            .map(agreement -> new ResponseDefinitionBuilder()
                .withStatus(409)
                .withHeader("Content-Type", "application/json")
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
                    return new ResponseDefinitionBuilder()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(response))
                        .build();
                }
            );
    }

    private ResponseDefinition accept(Request request) {
        return new ResponseDefinitionBuilder()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(toJson(Map.of(
                "summary", "Insufficient permissions",
                "detail", "Your company doesn't have permission to accept EUA. You'll have to use our default form for this action.",
                "status_code", 403
            )))
            .build();
    }

    private ResponseDefinition list(Request request) {
        return new ResponseDefinitionBuilder()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(toJson(sublist(request, agreements.values())))
            .build();
    }

    private ResponseDefinition getById(Request request) {
        String id = getIdFromPath(request.getUrl(), 5);
        EndUserAgreement entity = agreements.get(id);
        if (entity == null) {
            return new ResponseDefinitionBuilder()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(Map.of(
                    "summary", "Not found",
                    "detail", "End User Agreement " + id + " not found"
                )))
                .build();
        }

        return new ResponseDefinitionBuilder()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(toJson(entity))
            .build();
    }

    private ResponseDefinition deleteById(Request request) {
        String id = getIdFromPath(request.getUrl(), 5);
        EndUserAgreement entity = removeAgreement(id);

        if (entity == null) {
            return new ResponseDefinitionBuilder()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(Map.of(
                    "status_code", 404,
                    "summary", "Not found",
                    "detail", "End User Agreement " + id + " not found"
                )))
                .build();
        }

        return new ResponseDefinitionBuilder()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(toJson(Map.of(
                "status_code", 200,
                "summary", "End User Agreement deleted",
                "detail", "End User Agreement " + id + " deleted"
            )))
            .build();
    }
}
