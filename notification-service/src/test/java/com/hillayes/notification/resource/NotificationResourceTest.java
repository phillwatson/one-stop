package com.hillayes.notification.resource;

import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationId;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.repository.NotificationRepository;
import com.hillayes.notification.service.UserService;
import com.hillayes.onestop.api.NotificationResponse;
import com.hillayes.onestop.api.ServiceError;
import com.hillayes.onestop.api.ServiceErrorResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class NotificationResourceTest extends TestBase {
    private static final TypeRef<List<NotificationResponse>> NOTIFICATION_LIST = new TypeRef<>() {};

    @InjectMock
    UserService userService;

    @InjectMock
    NotificationRepository notificationRepository;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetNotifications() {
        // given: a user
        User user = User.builder()
            .id(UUID.fromString(userIdStr))
            .givenName(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        when(userService.getUser(user.getId()))
            .thenReturn(Optional.of(user));

        // and: a collection of notifications exist
        Instant start = Instant.now().minus(Duration.ofHours(10));
        List<Notification> notifications = mockNotifications(user.getId(), start);
        when(notificationRepository.listByUserAndTime(eq(user.getId()), any()))
            .thenReturn(notifications);

        // when: the resource is called
        Instant after = start.plus(Duration.ofHours(5));
        List<NotificationResponse> response = given()
            .request()
            .queryParam("after", after.toString())
            .contentType(JSON)
            .when()
            .get("/api/v1/notifications")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(NOTIFICATION_LIST);

        // then: the repository is called with correct parameters
        verify(notificationRepository).listByUserAndTime(user.getId(), after);

        // and: the result is not empty
        assertEquals(notifications.size(), response.size());

        // and: the result contains all notifications
        notifications.forEach(expected -> {
            assertNotNull(response.stream()
                .filter(notification -> notification.getId().equals(expected.getId()))
                .findFirst().orElse(null));
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteNotification_HappyPath() {
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
    public void testDeleteNotification_WrongUser() {
        // given: a notification to be deleted
        Notification notification = mockNotification(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        when(notificationRepository.findByIdOptional(notification.getId())).thenReturn(Optional.of(notification));

        // when: the resource is called
        // then: a not-found response is returned
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("id", notification.getId())
            .delete("/api/v1/notifications/{id}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract().as(ServiceErrorResponse.class);

        assertNotNull(response.getErrors());
        assertFalse(response.getErrors().isEmpty());
        ServiceError error = response.getErrors().get(0);

        // and: the error shows reason
        assertEquals("ENTITY_NOT_FOUND", error.getMessageId());

        // and: context attributes are present
        assertNotNull(error.getContextAttributes());
        assertFalse(error.getContextAttributes().isEmpty());

        // and: the entity type is identified
        assertEquals("Notification", error.getContextAttributes().get("entity-type"));

        // and: the notification ID is identified
        assertEquals(notification.getId().toString(), error.getContextAttributes().get("entity-id"));

        // and: no notification is deleted
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
    public void testDeleteNotification_WrongRole() {
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

    private Notification mockNotification(UUID userId, UUID id, Instant dateCreated) {
        return Notification.builder()
            .id(id)
            .userId(userId)
            .correlationId(randomAlphanumeric(20))
            .dateCreated(dateCreated)
            .messageId(NotificationId.CONSENT_EXPIRED)
            .build();
    }

    private List<Notification> mockNotifications(UUID userId, Instant startDateTime) {
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
