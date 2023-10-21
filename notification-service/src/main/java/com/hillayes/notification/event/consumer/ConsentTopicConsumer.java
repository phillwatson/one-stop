package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.*;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.NotificationId;
import com.hillayes.notification.service.NotificationService;
import com.hillayes.notification.task.SendEmailTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class ConsentTopicConsumer {
    private final SendEmailTask sendEmailTask;
    private final NotificationService notificationService;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void consume(@Observes(during = TransactionPhase.AFTER_SUCCESS)
                        @TopicObserved(Topic.CONSENT) EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received consent event [payloadClass: {}]", payloadClass);

        Map<String, Object> params = new HashMap<>();
        if (ConsentInitiated.class.getName().equals(payloadClass)) {
            // no action required
        }

        else if (ConsentGiven.class.getName().equals(payloadClass)) {
            ConsentGiven event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmailTask.queueJob(event.getUserId(), TemplateName.CONSENT_GIVEN, params);
        }

        else if (ConsentDenied.class.getName().equals(payloadClass)) {
            ConsentDenied event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmailTask.queueJob(event.getUserId(), TemplateName.CONSENT_DENIED, params);
            notificationService.createNotification(event.getUserId(), eventPacket.getTimestamp(),
                NotificationId.CONSENT_DENIED, params);
        }

        else if (ConsentCancelled.class.getName().equals(payloadClass)) {
            ConsentCancelled event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmailTask.queueJob(event.getUserId(), TemplateName.CONSENT_CANCELLED, params);
        }

        else if (ConsentSuspended.class.getName().equals(payloadClass)) {
            ConsentSuspended event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmailTask.queueJob(event.getUserId(), TemplateName.CONSENT_SUSPENDED, params);
            notificationService.createNotification(event.getUserId(), eventPacket.getTimestamp(),
                NotificationId.CONSENT_SUSPENDED, params);
        }

        else if (ConsentExpired.class.getName().equals(payloadClass)) {
            ConsentExpired event = eventPacket.getPayloadContent();
            params.put("event", event);
            sendEmailTask.queueJob(event.getUserId(), TemplateName.CONSENT_EXPIRED, params);
            notificationService.createNotification(event.getUserId(), eventPacket.getTimestamp(),
                NotificationId.CONSENT_EXPIRED, params);
        }
    }
}
