package com.hillayes.integration.api;

import com.hillayes.onestop.api.NotificationResponse;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class NotificationApi extends ApiBase {
    private static final TypeRef<List<NotificationResponse>> NOTIFICATION_LIST = new TypeRef<>() {};

    public NotificationApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public List<NotificationResponse> getNotifications(Instant after) {
        return givenAuth()
            .queryParam("after", after.toString())
            .get("/api/v1/notifications")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(NOTIFICATION_LIST);
    }

    public void deleteNotification(UUID notificationId) {
        deleteNotification(notificationId, 204);
    }

    public Response deleteNotification(UUID notificationId, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .delete("/api/v1/notifications/{id}", notificationId)
            .then()
            .statusCode(expectedStatus)
            .extract().response();
    }
}
