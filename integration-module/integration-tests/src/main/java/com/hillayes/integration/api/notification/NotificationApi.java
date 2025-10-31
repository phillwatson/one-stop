package com.hillayes.integration.api.notification;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.PaginatedNotifications;
import io.restassured.response.Response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class NotificationApi extends ApiBase {
    public NotificationApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedNotifications getNotifications(Instant after, int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("after", after.toString())
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/notifications")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedNotifications.class);
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
