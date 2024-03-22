package com.hillayes.notification.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationId;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class NotificationRepository extends RepositoryBase<Notification, UUID> {
    public Page<Notification> listByUserAndTime(UUID userId, Instant after, int pageIndex, int pageSize) {
        OrderBy orderBy = OrderBy.ascending("dateCreated");
        Map<String,Object> parameters = Map.of(
            "userId", userId,
            "after", after);
        return pageAll("userId = :userId AND dateCreated > :after", pageIndex, pageSize,
            orderBy, parameters);
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
