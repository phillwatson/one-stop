package com.hillayes.notification.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.jpa.Page;
import com.hillayes.events.events.consent.ConsentExpired;
import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationId;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.repository.NotificationRepository;
import com.hillayes.notification.service.UserService;
import com.hillayes.onestop.api.PaginatedNotifications;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class NotificationResourceTest extends TestBase {
    @Inject
    ObjectMapper jsonMapper;

    @InjectMock
    UserService userService;

    @InjectMock
    NotificationRepository notificationRepository;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetNotifications() throws JsonProcessingException {
        // given: a user
        User user = User.builder()
            .id(UUID.fromString(userIdStr))
            .givenName(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        when(userService.getUser(user.getId()))
            .thenReturn(Optional.of(user));

        int pageIndex = 0;
        int pageSize = 5;

        // and: a collection of notifications exist
        Instant start = Instant.now().minus(Duration.ofHours(10));
        List<Notification> notifications = mockNotifications(user.getId(), start);
        when(notificationRepository.listByUserAndTime(eq(user.getId()), any(), anyInt(), anyInt()))
            .thenReturn(Page.of(notifications, pageIndex, pageSize));

        // when: the resource is called
        Instant after = start.plus(Duration.ofHours(5));
        PaginatedNotifications response = given()
            .request()
            .queryParam("after", after.toString())
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .contentType(JSON)
            .when()
            .get("/api/v1/notifications")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedNotifications.class);

        // then: the repository is called with correct parameters
        verify(notificationRepository).listByUserAndTime(user.getId(), after, pageIndex, pageSize);

        // and: the response contains the notifications
        assertEquals(pageIndex, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertEquals(pageSize, response.getCount());
        assertEquals(notifications.size(), response.getTotal());
        assertEquals(3, response.getTotalPages());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteNotification_HappyPath() throws JsonProcessingException {
        // given: a user
        UUID userId = UUID.fromString(userIdStr);

        // and: a notification to be deleted
        Notification notification = mockNotification(userId, UUID.randomUUID(), Instant.now());
        when(notificationRepository.findByIdOptional(notification.getId())).thenReturn(Optional.of(notification));

        // when: the resource is called
        given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("id", notification.getId())
            .delete("/api/v1/notifications/{id}")
            .then()
            .statusCode(204);

        // then: the repository is called to retrieve the notification
        verify(notificationRepository).findByIdOptional(notification.getId());

        // and: the repository is called to delete the notification
        verify(notificationRepository).delete(notification);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteNotification_WrongUser() throws JsonProcessingException {
        // given: a notification to be deleted
        Notification notification = mockNotification(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        when(notificationRepository.findByIdOptional(notification.getId())).thenReturn(Optional.of(notification));

        // when: the resource is called
        given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("id", notification.getId())
            .delete("/api/v1/notifications/{id}")
            .then()
            .statusCode(204);

        // then: no notification is deleted
        verify(notificationRepository, never()).deleteById(any());
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteNotification_NotFound() {
        // given: the identified notification does not exist
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findByIdOptional(notificationId)).thenReturn(Optional.empty());

        // when: the resource is called
        // then: a success response is returned
        given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("id", notificationId)
            .delete("/api/v1/notifications/{id}")
            .then()
            .statusCode(204);

        // and: a call is made to read the notification
        verify(notificationRepository).findByIdOptional(notificationId);

        // and: no notification is deleted
        verify(notificationRepository, never()).deleteById(any());
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
    public void testDeleteNotification_WrongRole() throws JsonProcessingException {
        // given: a notification to be deleted
        Notification notification = mockNotification(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        when(notificationRepository.findByIdOptional(notification.getId())).thenReturn(Optional.of(notification));

        // when: the resource is called
        // then: a forbidden response is returned
        given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("id", notification.getId())
            .delete("/api/v1/notifications/{id}")
            .then()
            .statusCode(403);
    }

    private Notification mockNotification(UUID userId, UUID id, Instant dateCreated) throws JsonProcessingException {
        Map<String, Object> params = Map.of(
            "event", ConsentExpired.builder()
                .userId(userId)
                .consentId(UUID.randomUUID())
                .institutionId(UUID.randomUUID().toString())
                .institutionName(randomAlphanumeric(30))
                .build()
        );
        return Notification.builder()
            .id(id)
            .userId(userId)
            .correlationId(randomAlphanumeric(20))
            .dateCreated(dateCreated)
            .messageId(NotificationId.CONSENT_EXPIRED)
            .attributes(jsonMapper.writeValueAsString(params))
            .build();
    }

    private List<Notification> mockNotifications(UUID userId, Instant startDateTime) throws JsonProcessingException {
        Instant now = Instant.now();
        List<Notification> notifications = new ArrayList<>();

        Notification notification = mockNotification(userId, UUID.randomUUID(), startDateTime);
        while (notification.getDateCreated().isBefore(now)) {
            notifications.add(notification);
            notification = notification.toBuilder()
                .id(UUID.randomUUID())
                .correlationId(randomAlphanumeric(20))
                .dateCreated(notification.getDateCreated().plus(Duration.ofHours(1)))
                .build();
        }

        return notifications;
    }
}
