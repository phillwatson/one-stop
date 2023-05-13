package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.hillayes.rail.model.*;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.hillayes.rail.utils.TestData.toJson;

@Slf4j
public class NordigenSimulator {
    private final WireMockServer wireMockServer;

    private final Map<String, EntityStubs<EndUserAgreement>> endUserAgreements = new HashMap<>();
    private final Map<String, EntityStubs<Requisition>> requisitions = new HashMap<>();

    public NordigenSimulator() {
        wireMockServer = new WireMockServer(
            options()
                .port(8089)
                .notifier(log.isTraceEnabled() ? new ConsoleNotifier(true) : new NullNotifier())
                .extensions(
                    new DeleteAgreements(this),
                    new DeleteRequisitions(this),
                    new ListTransformer<>("agreementList", this.endUserAgreements),
                    new ListTransformer<>("requisitionList", this.requisitions)
                )
        );
    }

    Optional<EntityStubs<EndUserAgreement>> popAgreement(String agreementId) {
        return Optional.ofNullable(endUserAgreements.remove(agreementId));
    }

    Optional<EntityStubs<Requisition>> popRequisition(String requisitionId) {
        return Optional.ofNullable(requisitions.remove(requisitionId));
    }

    public void start() {
        wireMockServer.start();
    }

    public void stop() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    public void reset() {
        wireMockServer.resetAll();

        // set up minimum stubs
        login();
        listAgreements();
        listRequisitions();
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
                .withBody(toJson(response)))
        );
    }

    public EndUserAgreement stubAgreement(EndUserAgreementRequest request) {
        EndUserAgreement response = new EndUserAgreement();
        response.id = UUID.randomUUID().toString();
        response.maxHistoricalDays = request.getMaxHistoricalDays();
        response.accessValidForDays = request.getAccessValidForDays();
        response.accessScope = request.getAccessScope();
        response.institutionId = request.getInstitutionId();
        response.created = OffsetDateTime.now();
        response.accepted = null;

        EntityStubs<EndUserAgreement> stubs = new EntityStubs<>(response)
            // mock create endpoint
            .add(wireMockServer.stubFor(post(urlEqualTo("/api/v2/agreements/enduser/"))
                .willReturn(created()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(response)))
            ))

            // mock get endpoint
            .add(wireMockServer.stubFor(get(urlEqualTo("/api/v2/agreements/enduser/" + response.id + "/"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(response)))
            ))

            // mock accept endpoint
            .add(wireMockServer.stubFor(put(urlEqualTo("/api/v2/agreements/enduser/" + response.id + "/accept/"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(Map.of(
                        "summary", "Insufficient permissions",
                        "detail", "Your company doesn't have permission to accept EUA. You'll have to use our default form for this action.",
                        "status_code", 403
                    ))))
            ))

            // mock delete endpoint
            .add(wireMockServer.stubFor(delete(urlEqualTo("/api/v2/agreements/enduser/" + response.id + "/"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(Map.of(
                        "summary", "End User Agreement deleted",
                        "detail", "End User Agreement " + response.id + " deleted"
                    ))))
                .withPostServeAction(DeleteAgreements.class.getSimpleName(), Parameters.one("id", response.id))
            ));

        endUserAgreements.put(response.id, stubs);
        return response;
    }

    public Requisition stubRequisition(RequisitionRequest request) {
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

        EntityStubs<Requisition> stubs = new EntityStubs<>(response)
            // mock create endpoint
            .add(wireMockServer.stubFor(post(urlEqualTo("/api/v2/requisitions/"))
                .willReturn(created()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(response)))
            ))

            // mock get endpoint
            .add(wireMockServer.stubFor(get(urlEqualTo("/api/v2/requisitions/" + response.id + "/"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(response)))
            ))

            // mock delete endpoint
            .add(wireMockServer.stubFor(delete(urlEqualTo("/api/v2/requisitions/" + response.id + "/"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(Map.of(
                        "summary", "Requisition deleted",
                        "detail", "Requisition " + response.id + " deleted with all its End User Agreements"
                    ))))
                .withPostServeAction(DeleteRequisitions.class.getSimpleName(), Parameters.one("id", response.id))
            ));

        requisitions.put(response.id, stubs);
        return response;
    }

    private void listAgreements() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/agreements/enduser/"))
                .willReturn(
                    aResponse().withTransformers("agreementList")
                )
        );
    }

    private void listRequisitions() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/requisitions/"))
                .willReturn(
                    aResponse().withTransformers("requisitionList")
                )
        );
    }

    private static class NullNotifier implements Notifier {
        public void info(String message) {}
        public void error(String message) {}
        public void error(String message, Throwable t) {}
    }
}
