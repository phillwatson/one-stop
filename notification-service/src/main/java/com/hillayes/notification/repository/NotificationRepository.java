package com.hillayes.notification.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationId;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class NotificationRepository extends RepositoryBase<Notification, UUID> {
    public List<Notification> listByUserAndTime(UUID userId, Instant after) {
        return find("userId = :userId AND dateCreated > :after",
            Parameters
                .with("userId", userId)
                .and("after", after))
            .list();
    }

    public Optional<Notification> findByUserAndTimestamp(UUID userId, Instant timestamp, NotificationId notificationId) {
        return find("userId = :userId AND dateCreated = :timestamp AND messageId = :notificationId",
            Parameters
                .with("userId", userId)
                .and("timestamp", timestamp)
                .and("notificationId", notificationId))
            .firstResultOptional();
    }
}
