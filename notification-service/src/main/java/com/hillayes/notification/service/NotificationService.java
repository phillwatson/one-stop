package com.hillayes.notification.service;

import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.repository.NotificationRepository;
import com.hillayes.exception.common.NotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class NotificationService {
    @Inject
    NotificationRepository notificationRepository;

    public List<Notification> listNotifications(UUID userId, Instant after) {
        log.info("List notifications [userId: {}, after: {}]", userId, after);

        List<Notification> notifications = notificationRepository.listByUserAndTime(userId, after);

        log.debug("List notifications [userId: {}, after: {}, count: {}]", userId, after, notifications.size());
        return notifications;
    }

    public void deleteNotification(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId);
        if (notification == null) {
            return;
        }

        if (!notification.getUserId().equals(userId)) {
            throw new NotFoundException("Notification", notificationId);
        }

        notificationRepository.delete(notification);
    }
}
