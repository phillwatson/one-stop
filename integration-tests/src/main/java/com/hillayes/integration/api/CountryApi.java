package com.hillayes.integration.api;

import com.hillayes.onestop.api.CountryResponse;
import com.hillayes.onestop.api.PaginatedCountries;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.BINARY;

public class CountryApi extends ApiBase {
    public CountryApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedCountries getCountries(int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/rails/countries")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedCountries.class);
    }

    public CountryResponse getCountry(String countryId) {
        return givenAuth()
            .get("/api/v1/rails/countries/{countryId}", countryId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(CountryResponse.class);
    }

    public FileResponse getCountryLogo(String countryId) {
        Response response = givenAuth()
            .get("/api/v1/rails/countries/{countryId}/logos", countryId)
            .then()
            .statusCode(200)
            .contentType(BINARY)
            .extract().response();

        String header = response.header("Content-Disposition");
        int startIndex = header.indexOf("filename=\"") + "filename=\"".length();
        int endIndex = header.indexOf('"', startIndex);
        String filename = header.substring(startIndex, endIndex);
        return new FileResponse(filename, response.asByteArray());
    }
}
