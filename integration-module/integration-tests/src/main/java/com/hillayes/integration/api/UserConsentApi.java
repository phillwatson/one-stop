package com.hillayes.integration.api;

import com.hillayes.onestop.api.PaginatedUserConsents;
import com.hillayes.onestop.api.UserConsentRequest;
import com.hillayes.onestop.api.UserConsentResponse;
import io.restassured.response.Response;

import java.net.URI;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;

public class UserConsentApi extends ApiBase {
    public UserConsentApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedUserConsents getConsents(int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/rails/consents")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedUserConsents.class);
    }

    public UserConsentResponse getConsentForInstitution(String institutionId) {
        return getConsentForInstitution(institutionId, 200)
            .as(UserConsentResponse.class);
    }

    public Response getConsentForInstitution(String institutionId, int expectedStatus) {
        return givenAuth()
            .get("/api/v1/rails/consents/{institutionId}", institutionId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public void deleteConsent(String institutionId, boolean purge) {
        deleteConsent(institutionId, purge, 204);
    }

    public Response deleteConsent(String institutionId, boolean purge, int expectedStatus) {
        return givenAuth()
            .queryParam("purge", purge)
            .delete("/api/v1/rails/consents/{institutionId}", institutionId)
            .then()
            .statusCode(expectedStatus)
            .extract().response();
    }

    public URI register(String institutionId, UserConsentRequest consentRequest) {
        return URI.create(
            register(institutionId, consentRequest, 201).header("Location")
        );
    }

    public Response register(String institutionId, UserConsentRequest consentRequest, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(consentRequest)
            .post("/api/v1/rails/consents/{institutionId}", institutionId)
            .then()
            .statusCode(expectedStatus)
            .extract().response();
    }

    public Response consentResponse(String railProvider,
                                    String userConsentId,
                                    String error,
                                    String details) {
        return givenAuth()
            .redirects().follow(false)
            .queryParam("ref", userConsentId)
            .queryParam("error", error)
            .queryParam("details", details)
            .get("/api/v1/rails/consents/response/{railProvider}", railProvider)
            .then()
            .statusCode(307)
            .extract().response();
    }
}
