package com.hillayes.notification.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.notification.domain.Notification;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NotificationRepository extends RepositoryBase<Notification, UUID> {
    public List<Notification> listByUserAndTime(UUID userId, Instant after) {
        return find("accountId = :userID AND dateCreated > :after",
            Parameters
                .with("userId", userId)
                .and("after", after))
            .list();
    }
}
