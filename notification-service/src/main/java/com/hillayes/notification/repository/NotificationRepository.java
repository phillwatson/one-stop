package com.hillayes.notification.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationId;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class NotificationRepository extends RepositoryBase<Notification, UUID> {
    public List<Notification> listByUserAndTime(UUID userId, Instant after) {
        return listAll("userId = :userId AND dateCreated > :after",
            Map.of(
                "userId", userId,
                "after", after)
        );
    }

    public Optional<Notification> findByUserAndTimestamp(UUID userId, Instant timestamp, NotificationId notificationId) {
        return findFirst("userId = :userId AND dateCreated = :timestamp AND messageId = :notificationId",
            Map.of(
                "userId", userId,
                "timestamp", timestamp,
                "notificationId", notificationId)
        );
    }
}
