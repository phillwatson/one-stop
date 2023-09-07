package com.hillayes.notification.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.notification.domain.Notification;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class NotificationRepository extends RepositoryBase<Notification, UUID> {
}
