package com.hillayes.notification.repository;

import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationId;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
@Slf4j
public class NotificationRepositoryTest {
    @Inject
    NotificationRepository fixture;

    @Test
    public void testSave() {
        // given: a notification record
        Notification notification = Notification.builder()
            .userId(UUID.randomUUID())
            .dateCreated(Instant.now())
            .messageId(NotificationId.CONSENT_EXPIRED)
            .build();

        // when: the notification is saved
        Notification saved = fixture.saveAndFlush(notification);

        // then: the notification is assigned an ID
        assertNotNull(saved.getId());

        // clear orm cache
        fixture.getEntityManager().clear();

        // when: the notification is read
        Notification reloaded = fixture.findById(saved.getId());

        // then: the ID are the same
        assertEquals(saved.getId(), reloaded.getId());
    }

    @Test
    public void testDeleteNotification() {
        // given: a notification record
        Notification notification = Notification.builder()
            .userId(UUID.randomUUID())
            .dateCreated(Instant.now())
            .messageId(NotificationId.CONSENT_EXPIRED)
            .build();

        // and: the notification is saved
        Notification saved = fixture.saveAndFlush(notification);

        // clear orm cache
        fixture.getEntityManager().clear();

        // when: the notification is deleted
        fixture.deleteById(saved.getId());

        // and: the cache is flushed
        fixture.flush();

        // then: the notification can no longer be read
        assertNull(fixture.findById(saved.getId()));

        // and: no attributes can be found in the database
        Long count = fixture.getEntityManager()
            .createQuery("select count(*) from NotificationAttribute where notification.id = :id",
                Long.class)
            .setParameter("id", saved.getId())
            .getSingleResult();
        assertEquals(0, count);
    }
}
