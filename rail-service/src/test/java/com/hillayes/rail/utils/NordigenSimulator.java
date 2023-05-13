package com.hillayes.rail.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import com.hillayes.rail.model.*;

import java.time.OffsetDateTime;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class NordigenSimulator {
    private final WireMockServer wireMockServer;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public NordigenSimulator() {
        wireMockServer = new WireMockServer(
            options()
                .port(8089)
                .extensions(new DeleteStubs())
                .notifier(new ConsoleNotifier(true)));
        //.notifier(new Slf4jNotifier(true))
    }

    public void start() {
        wireMockServer.start();
    }

    public String json(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    public void reset() {
        wireMockServer.resetAll();
        login();
    }

    public void login() {
        ObtainJwtResponse response = new ObtainJwtResponse();
        response.access = UUID.randomUUID().toString();
        response.accessExpires = 3600;
        response.refresh = UUID.randomUUID().toString();
        response.refreshExpires = 7200;

        wireMockServer.stubFor(post(urlEqualTo("/api/v2/token/new/"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(json(response)))
        );
    }

    public EndUserAgreement createAgreement(EndUserAgreementRequest request) {
        EndUserAgreement response = new EndUserAgreement();
        response.id = UUID.randomUUID().toString();
        response.maxHistoricalDays = request.getMaxHistoricalDays();
        response.accessValidForDays = request.getAccessValidForDays();
        response.accessScope = request.getAccessScope();
        response.institutionId = request.getInstitutionId();
        response.created = OffsetDateTime.now();
        response.accepted = null;

        DeleteStubs.StubbingList stubbings = new DeleteStubs.StubbingList()
            .add(wireMockServer.stubFor(post(urlEqualTo("/api/v2/agreements/enduser/"))
                .willReturn(created()
                    .withHeader("Content-Type", "application/json")
                    .withBody(json(response)))
            ))

            .add(wireMockServer.stubFor(get(urlEqualTo("/api/v2/agreements/enduser/" + response.id + "/"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(json(response)))
            ))

            .add(wireMockServer.stubFor(put(urlEqualTo("/api/v2/agreements/enduser/" + response.id + "/accept/"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(json(Map.of(
                        "summary", "Insufficient permissions",
                        "detail", "Your company doesn't have permission to accept EUA. You'll have to use our default form for this action.",
                        "status_code", 403
                    ))))
            ));

        wireMockServer.stubFor(delete(urlEqualTo("/api/v2/agreements/enduser/" + response.id + "/"))
            .willReturn(ok()
                .withHeader("Content-Type", "application/json")
                .withBody(json(Map.of(
                    "summary", "End User Agreement deleted",
                    "detail", "End User Agreement " + response.id + " deleted"
                ))))
            .withPostServeAction("DeleteStubs", stubbings)
        );

        return response;
    }

    public void listAgreements(List<EndUserAgreement> agreements) {
        PaginatedList<EndUserAgreement> response = new PaginatedList<>();
        response.count = agreements.size();
        response.next = null;
        response.previous = null;
        response.results = agreements;

        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/agreements/enduser/"))
                .andMatching(request -> {
                    QueryParameter offsetParam = request.queryParameter("offset");
                    int offset = offsetParam.isPresent()
                        ? Math.max(0, Integer.parseInt(offsetParam.firstValue())) : 0;

                    QueryParameter limitParam = request.queryParameter("limit");
                    int limit = limitParam.isPresent()
                        ? Math.max(0, Integer.parseInt(limitParam.firstValue())) : Integer.MAX_VALUE;

                    if ((limit <= 0) || (offset >= agreements.size())) {
                        response.results = Collections.emptyList();
                    } else {
                        agreements.subList(offset, Math.min(offset + limit, agreements.size()));
                    }
                    return MatchResult.exactMatch();
                })
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(json(response)))
        );
    }

    public Requisition createRequisition(RequisitionRequest request) {
        String id = UUID.randomUUID().toString();
        Requisition response = Requisition.builder()
            .id(id)
            .created(OffsetDateTime.now())
            .status(RequisitionStatus.CR)
            .link("https://ob.nordigen.com/psd2/start/" + id + "/" + request.getInstitutionId())
            .redirect(request.getRedirect())
            .institutionId(request.getInstitutionId())
            .agreement(request.getAgreement())
            .reference(request.getReference())
            .userLanguage(request.getUserLanguage())
            .ssn(request.getSsn())
            .accountSelection(request.getAccountSelection())
            .redirectImmediate(request.getRedirectImmediate())
            .accounts(List.of())
            .build();

        DeleteStubs.StubbingList stubbings = new DeleteStubs.StubbingList()
            .add(wireMockServer.stubFor(post(urlEqualTo("/api/v2/requisitions/"))
                .willReturn(created()
                    .withHeader("Content-Type", "application/json")
                    .withBody(json(response)))
            ))

            .add(wireMockServer.stubFor(get(urlEqualTo("/api/v2/requisitions/" + response.id + "/"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(json(response)))
            ));

        wireMockServer.stubFor(delete(urlEqualTo("/api/v2/requisitions/" + response.id + "/"))
            .willReturn(ok()
                .withHeader("Content-Type", "application/json")
                .withBody(json(Map.of(
                    "summary", "Requisition deleted",
                    "detail", "Requisition " + response.id + " deleted with all its End User Agreements"
                ))))
            .withPostServeAction("DeleteStubs", stubbings)
        );

        return response;
    }

    public void listRequisitions(List<Requisition> requisitions) {
        PaginatedList<Requisition> response = new PaginatedList<>();
        response.count = requisitions.size();
        response.next = null;
        response.previous = null;
        response.results = requisitions;

        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/requisitions/"))
                .andMatching(request -> {
                    QueryParameter offsetParam = request.queryParameter("offset");
                    int offset = offsetParam.isPresent()
                        ? Math.max(0, Integer.parseInt(offsetParam.firstValue())) : 0;

                    QueryParameter limitParam = request.queryParameter("limit");
                    int limit = limitParam.isPresent()
                        ? Math.max(0, Integer.parseInt(limitParam.firstValue())) : Integer.MAX_VALUE;

                    if ((limit == 0) || (offset >= requisitions.size())) {
                        response.results = Collections.emptyList();
                    } else {
                        requisitions.subList(offset, Math.min(offset + limit, requisitions.size()));
                    }
                    return MatchResult.exactMatch();
                })
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(json(response)))
        );
    }
}
