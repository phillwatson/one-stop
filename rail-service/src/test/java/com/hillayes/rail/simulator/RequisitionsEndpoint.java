package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.hillayes.rail.model.Requisition;
import com.hillayes.rail.model.RequisitionRequest;
import com.hillayes.rail.model.RequisitionStatus;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.hillayes.rail.utils.TestData.fromJson;
import static com.hillayes.rail.utils.TestData.toJson;

@Singleton
@Slf4j
public class RequisitionsEndpoint extends AbstractResponseTransformer {
    @Inject
    AgreementsEndpoint agreements;

    private final Map<String, Requisition> requisitions = new HashMap<>();

    public RequisitionsEndpoint() {
        super(null);
    }

    public void register(WireMockServer wireMockServer) {
        requisitions.clear();

        // mock create endpoint
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/requisitions/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(RequisitionsEndpoint.class.getSimpleName())
            )
        );

        // mock list endpoint
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v2/requisitions/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(RequisitionsEndpoint.class.getSimpleName())
                    .withTransformerParameter("list", true)
            )
        );

        // mock get endpoint
        wireMockServer.stubFor(get(urlPathMatching("/api/v2/requisitions/(.*)/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(RequisitionsEndpoint.class.getSimpleName())
            )
        );

        // mock accept endpoint
        wireMockServer.stubFor(put(urlPathMatching("/api/v2/requisitions/(.*)/accept/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(RequisitionsEndpoint.class.getSimpleName())
            )
        );

        // mock delete endpoint
        wireMockServer.stubFor(delete(urlPathMatching("/api/v2/requisitions/(.*)/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(RequisitionsEndpoint.class.getSimpleName())
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

        if (method.equals(RequestMethod.GET)) {
            if (parameters.getBoolean("list", false)) {
                return list(request, responseDefinition);
            }
            return getById(request, responseDefinition);
        }

        if (method.equals(RequestMethod.DELETE)) {
            return deleteById(request, responseDefinition);
        }

        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(400)
            .withBody(toJson(Map.of(
                "status_code", 400,
                "summary", "Unsupported method",
                "detail", "This endpoint does not support the " + method + " method."
            )))
            .build();
    }

    private ResponseDefinition create(Request request,
                                      ResponseDefinition responseDefinition) {
        RequisitionRequest requisitionRequest = fromJson(request.getBodyAsString(), RequisitionRequest.class);

        return requisitions.values().stream()
            .filter(agreement -> agreement.institutionId.equals(requisitionRequest.getInstitutionId()))
            .map(agreement -> ResponseDefinitionBuilder.like(responseDefinition)
                .withStatus(409)
                .withBody(toJson(Map.of(
                    "summary", "Conflict",
                    "detail", "An requisition with this Institution already exists.",
                    "status_code", 409
                )))
                .build()
            )
            .findFirst()
            .orElseGet(() -> {
                String id = UUID.randomUUID().toString();
                Requisition response = Requisition.builder()
                    .id(id)
                    .created(OffsetDateTime.now())
                    .status(RequisitionStatus.CR)
                    .link("https://ob.nordigen.com/psd2/start/" + id + "/" + requisitionRequest.getInstitutionId())
                    .redirect(requisitionRequest.getRedirect())
                    .institutionId(requisitionRequest.getInstitutionId())
                    .agreement(requisitionRequest.getAgreement())
                    .reference(requisitionRequest.getReference())
                    .userLanguage(requisitionRequest.getUserLanguage())
                    .ssn(requisitionRequest.getSsn())
                    .accountSelection(requisitionRequest.getAccountSelection())
                    .redirectImmediate(requisitionRequest.getRedirectImmediate())
                    .accounts(List.of())
                    .build();

                requisitions.put(response.id, response);
                return ResponseDefinitionBuilder.like(responseDefinition)
                        .withStatus(201)
                        .withBody(toJson(response))
                        .build();
                }
            );
    }

    private ResponseDefinition list(Request request,
                                    ResponseDefinition responseDefinition) {
        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(sublist(request, requisitions.values())))
            .build();
    }

    private ResponseDefinition getById(Request request,
                                       ResponseDefinition responseDefinition) {
        String id = getIdFromPath(request.getUrl(), 4);
        Requisition entity = requisitions.get(id);
        if (entity == null) {
            return ResponseDefinitionBuilder.like(responseDefinition)
                .withStatus(404)
                .withBody(toJson(Map.of(
                    "summary", "Not found",
                    "detail", "Requisition " + id + " not found"
                )))
                .build();
        }

        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(entity))
            .build();
    }

    private ResponseDefinition deleteById(Request request,
                                          ResponseDefinition responseDefinition) {
        String id = getIdFromPath(request.getUrl(), 4);
        Requisition entity = requisitions.remove(id);

        if (entity == null) {
            return ResponseDefinitionBuilder.like(responseDefinition)
                .withStatus(404)
                .withBody(toJson(Map.of(
                    "status_code", 404,
                    "summary", "Not found",
                    "detail", "Requisition " + id + " not found"
                )))
                .build();
        }

        // delete associated agreements
        agreements.removeAgreement(entity.agreement);
        
        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(toJson(Map.of(
                "status_code", 200,
                "summary", "Requisition deleted",
                "detail", "Requisition " + id + " deleted"
            )))
            .build();
    }
}
