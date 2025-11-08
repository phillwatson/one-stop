package com.hillayes.alphavantage.api.service;

import com.hillayes.alphavantage.api.domain.ApiFunction;
import com.hillayes.alphavantage.api.domain.DailyTimeSeries;
import com.hillayes.alphavantage.api.domain.TickerSearchResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

/**
 * https://www.alphavantage.co/documentation/
 */
@ApplicationScoped
public class AlphaVantageApi {
    @ConfigProperty(name = "quarkus.rest-client.alpha-vantage-api.url")
    String BASE_URI;

    @ConfigProperty(name = "one-stop.alpha-vantage-api.secret-key", defaultValue = "not-set")
    String API_KEY;

    private RequestSpecification givenAuth(ApiFunction function) {
        return given()
            .baseUri(BASE_URI)
            .port(443)
            .queryParam("apikey", API_KEY)
            .queryParam("function", function);
    }

    public DailyTimeSeries getDailySeries(String stockSymbol) {
        return givenAuth(ApiFunction.TIME_SERIES_DAILY)
            .queryParam("symbol", stockSymbol)
            .get("query")
            .then()
            .contentType(JSON)
            .statusCode(200)
            .extract().as(DailyTimeSeries.class);
    }

    public TickerSearchResponse symbolSearch(String keywords) {
        return givenAuth(ApiFunction.SYMBOL_SEARCH)
            .queryParam("keywords", keywords)
            .get("query")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(TickerSearchResponse.class);
    }
}
