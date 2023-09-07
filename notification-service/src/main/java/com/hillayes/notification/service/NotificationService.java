package com.hillayes.notification.service;

import com.hillayes.notification.repository.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Transactional
@Slf4j
public class NotificationService {
    @Inject
    NotificationRepository notificationRepository;
}
