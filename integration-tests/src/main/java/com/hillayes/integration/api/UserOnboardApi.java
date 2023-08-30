package com.hillayes.integration.api;

import com.hillayes.onestop.api.UserCompleteRequest;
import com.hillayes.onestop.api.UserRegisterRequest;
import io.restassured.response.Response;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public class UserOnboardApi extends ApiBase {
    public UserOnboardApi() {
        super(null);
    }

    public void registerUser(UserRegisterRequest request) {
        given()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/users/onboard/register")
            .then()
            .statusCode(202);
    }

    /**
     * Completes a user's onboarding; passing the magic-link token, given in the
     * registration email, and the user's chosen given-name and password.
     *
     * @param request the magic-link token, given-name and password data.
     * @return the new user's id and auth-tokens.
     */
    public Pair<UUID, Map<String,String>> onboardUser(UserCompleteRequest request) {
        Response response = given()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/users/onboard/complete")
            .then()
            .statusCode(201)
            .extract().response();

        // extract the User ID from the response header
        String location = response.getHeader("Location");
        int index = location.lastIndexOf('/');
        UUID userId = UUID.fromString(location.substring(index + 1));

        return Pair.of(userId, response.cookies());
    }
}
