package com.hillayes.notification.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationId;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
@Slf4j
public class NotificationRepositoryTest {
    @Inject
    NotificationRepository fixture;

    @Test
    public void testListByUserAndTime() {
        // given: a collection of notifications for an identified user
        UUID userId = UUID.randomUUID();
        Instant start = Instant.now().minus(Duration.ofHours(10));
        mockNotifications(userId, start);

        // and: a collection of notifications for other users
        mockNotifications(UUID.randomUUID(), start);
        mockNotifications(UUID.randomUUID(), start);

        // and: a required start date-time
        Instant after = start.plus(Duration.ofHours(5));

        // when: the fixture is called
        Page<Notification> results = fixture.listByUserAndTime(userId, after, 0, 1000);

        // then: the result is not empty
        assertFalse(results.isEmpty());

        // and: the result contains only those for the identified user
        results.forEach(notification -> assertEquals(userId, notification.getUserId()));

        // and: only those after the given date-time are returned
        results.forEach(notification -> assertTrue(notification.getDateCreated().isAfter(after)));
    }

    @Test
    public void testFindByUserIdAndTimestamp() {
        // given: a collection of notifications for an identified user
        UUID userId = UUID.randomUUID();
        Instant start = Instant.now().minus(Duration.ofHours(10));
        List<Notification> notifications = mockNotifications(userId, start);

        // and: a collection of notifications for other users
        mockNotifications(UUID.randomUUID(), start);
        mockNotifications(UUID.randomUUID(), start);

        // when: each notification is queried
        notifications.forEach(expected -> {
            Optional<Notification> result = fixture.findByUserAndTimestamp(userId, expected.getDateCreated(), expected.getMessageId());

            // then: the result is returned
            assertTrue(result.isPresent());

            // and: the result matches the expectation
            Notification actual = result.get();
            assertEquals(expected.getId(), actual.getId());
            assertEquals(userId, actual.getUserId());
        });

    }

    private List<Notification> mockNotifications(UUID userId, Instant startDateTime) {
        Notification notification = Notification.builder()
            .userId(userId)
            .dateCreated(startDateTime)
            .messageId(NotificationId.CONSENT_EXPIRED)
            .build();

        Instant now = Instant.now();
        List<Notification> notifications = new ArrayList<>();
        while (notification.getDateCreated().isBefore(now)) {
            notifications.add(notification);
            notification = notification.toBuilder()
                .dateCreated(notification.getDateCreated().plus(Duration.ofHours(1)))
                .build();
        }

        fixture.saveAll(notifications);
        fixture.flush();

        return notifications;
    }
}
