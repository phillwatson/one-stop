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
    /**
     * Returns a page of notifications for the identified user, after the specified
     * date-time, ordered oldest first.
     * @param userId the user to which the notifications relate.
     * @param after the date-time after which notifications are to be returned.
     * @param pageIndex the page index to be returned.
     * @param pageSize the max number of notifications to be returned.
     * @return the page of notifications.
     */
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
