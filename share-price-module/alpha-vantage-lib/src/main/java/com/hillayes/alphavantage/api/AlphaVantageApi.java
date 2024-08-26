package com.hillayes.alphavantage.api;

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

    @ConfigProperty(name = "one-stop.alpha-vantage.secret.key", defaultValue = "not-set")
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
            .statusCode(200)
            .contentType(JSON)
            .extract().as(DailyTimeSeries.class);
    }
}
