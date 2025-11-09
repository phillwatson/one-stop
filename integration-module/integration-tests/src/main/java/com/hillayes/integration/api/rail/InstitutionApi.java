package com.hillayes.integration.api.rail;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.InstitutionResponse;
import com.hillayes.onestop.api.PaginatedInstitutions;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;

public class InstitutionApi extends ApiBase {
    public InstitutionApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedInstitutions getInstitutions(int pageIndex, int pageSize, String countryCode) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .queryParam("country", countryCode)
            .get("/api/v1/rails/institutions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedInstitutions.class);
    }

    public InstitutionResponse getInstitution(String institutionId) {
        return givenAuth()
            .get("/api/v1/rails/institutions/{institutionId}", institutionId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(InstitutionResponse.class);
    }
}
