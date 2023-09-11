package com.hillayes.notification.event.consumer;

import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.NotificationId;
import com.hillayes.notification.service.NotificationService;
import com.hillayes.notification.service.SendEmailService;
import com.hillayes.notification.service.UserService;
import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.*;
import com.hillayes.notification.task.QueueEmailTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@TopicConsumer(Topic.CONSENT)
@RequiredArgsConstructor
@Slf4j
public class ConsentTopicConsumer implements EventConsumer {
    private final QueueEmailTask queueEmailTask;
    private final NotificationService notificationService;

    @Transactional
    public void consume(EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received consent event [payloadClass: {}]", payloadClass);

        Map<String, Object> params = new HashMap<>();
        if (ConsentInitiated.class.getName().equals(payloadClass)) {
            // no action required
        }

        else if (ConsentGiven.class.getName().equals(payloadClass)) {
            ConsentGiven event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmail(event.getUserId(), TemplateName.CONSENT_GIVEN, params);
        }

        else if (ConsentDenied.class.getName().equals(payloadClass)) {
            ConsentDenied event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmail(event.getUserId(), TemplateName.CONSENT_DENIED, params);
        }

        else if (ConsentCancelled.class.getName().equals(payloadClass)) {
            ConsentCancelled event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmail(event.getUserId(), TemplateName.CONSENT_CANCELLED, params);
        }

        else if (ConsentSuspended.class.getName().equals(payloadClass)) {
            ConsentSuspended event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmail(event.getUserId(), TemplateName.CONSENT_SUSPENDED, params);

            notificationService.createNotification(event.getUserId(), NotificationId.CONSENT_SUSPENDED, params);
        }

        else if (ConsentExpired.class.getName().equals(payloadClass)) {
            ConsentExpired event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmail(event.getUserId(), TemplateName.CONSENT_EXPIRED, params);

            notificationService.createNotification(event.getUserId(), NotificationId.CONSENT_EXPIRED, params);
        }
    }

    private void sendEmail(UUID userId, TemplateName templateName, Map<String, Object> params) {
        queueEmailTask.queueJob(userId, templateName, params);
    }
}
