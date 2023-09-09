package com.hillayes.notification.repository;

import com.hillayes.events.domain.Topic;
import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationAttribute;
import com.hillayes.notification.domain.NotificationMessageId;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestTransaction
public class NotificationRepositoryTest {
    @Inject
    NotificationRepository fixture;

    @Test
    public void test() {
        // given: a notification record
        Notification notification = Notification.builder()
            .userId(UUID.randomUUID())
            .topic(Topic.CONSENT)
            .dateCreated(Instant.now())
            .messageId(NotificationMessageId.CONSENT_EXPIRED)
            .build();
        notification.addAttr("attr1", "value1");
        notification.addAttr("attr2", "value2");

        // when: the notification is saved
        Notification saved = fixture.save(notification);

        // then: the notification
        assertNotNull(saved.getId());

        saved.getAttributes().forEach(attr -> {
            assertNotNull(attr.getId());
            assertEquals(saved, attr.getNotification());
        });
    }
}
