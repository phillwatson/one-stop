package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.hillayes.rail.model.*;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.hillayes.rail.utils.TestData.toJson;

/**
 * A simulator for the Nordigen API. This simulator is used to stub out the Nordigen
 * API so that the Rail service can be tested without having to make calls to the
 * real Nordigen API.
 */
@Slf4j
public class NordigenSimulator {
    private final WireMockServer wireMockServer;

    private final Map<String, EntityStubs<EndUserAgreement>> endUserAgreements = new HashMap<>();
    private final Map<String, EntityStubs<Requisition>> requisitions = new HashMap<>();

    public NordigenSimulator(int portNumber) {
        wireMockServer = new WireMockServer(
            options()
                .port(portNumber)
                .notifier(log.isDebugEnabled() ? new ConsoleNotifier(true) : new NullNotifier())
                .extensions(
                    new DeleteAgreements(this),
                    new DeleteRequisitions(this),
                    new ListTransformer<>("agreementList", this.endUserAgreements),
                    new ListTransformer<>("requisitionList", this.requisitions)
                )
        );
    }

    /**
     * Used by the DeleteStubsExtension classes to retrieve the stubs for identified
     * EndUserAgreement and delete them. This will also remove the stubs from the
     * simulator's internal map of stubs.
     *
     * @param agreementId the ID of the agreement to retrieve.
     * @return the stubs for the agreement.
     */
    Optional<EntityStubs<EndUserAgreement>> popAgreement(String agreementId) {
        return Optional.ofNullable(endUserAgreements.remove(agreementId));
    }

    /**
     * Used by the DeleteStubsExtension classes to retrieve the stubs for the identified
     * Requisition and delete them. This will also remove the stubs from the simulator's
     * internal map of stubs.
     *
     * @param requisitionId the ID of the requisition to retrieve.
     * @return the stubs for the requisition.
     */
    Optional<EntityStubs<Requisition>> popRequisition(String requisitionId) {
        return Optional.ofNullable(requisitions.remove(requisitionId));
    }

    /**
     * Starts the wiremock server. Typically called at the start of each test class.
     */
    public void start() {
        wireMockServer.start();
    }

    /**
     * Stops the wiremock server. Typically called at the end of each test class.
     */
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
        login();
        listAgreements();
        listRequisitions();
    }

    /**
     * Mocks the endpoint to obtain access and refresh tokens from Nordigen.
     */
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

    /**
     * Mocks all endpoints related to the creation, retrieval, and deletion the given
     * EndUserAgreement.
     *
     * @param request the EndUserAgreementRequest to mock.
     * @return the EndUserAgreement that was stubbed.
     */
    public EndUserAgreement stubAgreement(EndUserAgreementRequest request) {
        EndUserAgreement response = new EndUserAgreement();
        response.id = UUID.randomUUID().toString();
        response.maxHistoricalDays = request.getMaxHistoricalDays();
        response.accessValidForDays = request.getAccessValidForDays();
        response.accessScope = request.getAccessScope();
        response.institutionId = request.getInstitutionId();
        response.created = OffsetDateTime.now();
        response.accepted = null;

        // create mock endpoints specific to this instance
        EntityStubs<EndUserAgreement> stubs = new EntityStubs<>(response)
            // mock create endpoint
            .add(wireMockServer.stubFor(post(urlEqualTo("/api/v2/agreements/enduser/"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.institution_id", equalTo(request.getInstitutionId())))
                .willReturn(created()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(response)))
            ))

            // mock get endpoint
            .add(wireMockServer.stubFor(get(urlEqualTo("/api/v2/agreements/enduser/" + response.id + "/"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(response)))
            ))

            // mock accept endpoint
            .add(wireMockServer.stubFor(put(urlEqualTo("/api/v2/agreements/enduser/" + response.id + "/accept/"))
                .withHeader("Content-Type", equalTo("application/json"))
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
                .withHeader("Content-Type", equalTo("application/json"))
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

    /**
     * Mocks all endpoints related to the creation, retrieval, and deletion the given
     * RequisitionRequest.
     *
     * @param request the RequisitionRequest to mock.
     * @return the Requisition that was stubbed.
     */
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

        // create mock endpoints specific to this instance
        EntityStubs<Requisition> stubs = new EntityStubs<>(response)
            // mock create endpoint
            .add(wireMockServer.stubFor(post(urlEqualTo("/api/v2/requisitions/"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.institution_id", equalTo(request.getInstitutionId())))
                .willReturn(created()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(response)))
            ))

            // mock get endpoint
            .add(wireMockServer.stubFor(get(urlEqualTo("/api/v2/requisitions/" + response.id + "/"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(toJson(response)))
            ))

            // mock delete endpoint
            .add(wireMockServer.stubFor(delete(urlEqualTo("/api/v2/requisitions/" + response.id + "/"))
                .withHeader("Content-Type", equalTo("application/json"))
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

    /**
     * Mocks all endpoints related to the retrieval of a list of EndUserAgreements.
     * The response will be based on the EndUserAgreementRequests that have been
     * stubbed since the most recent call to {@link #reset()}.
     */
    private void listAgreements() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/agreements/enduser/"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(
                    aResponse().withTransformers("agreementList")
                )
        );
    }

    /**
     * Mocks all endpoints related to the retrieval of a list of Requisitions.
     * The response will be based on the RequisitionRequests that have been
     * stubbed since the most recent call to {@link #reset()}.
     */
    private void listRequisitions() {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v2/requisitions/"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(
                    aResponse().withTransformers("requisitionList")
                )
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
