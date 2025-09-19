package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.audit.AuditIssuesFound;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.NotificationId;
import com.hillayes.notification.service.NotificationService;
import com.hillayes.notification.task.SendEmailTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@TopicConsumer(Topic.TRANSACTION_AUDIT)
@RequiredArgsConstructor
@Slf4j
public class TransactionAuditTopicConsumer implements EventConsumer {
    private final SendEmailTask sendEmailTask;
    private final NotificationService notificationService;

    @Transactional
    public void consume(EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user event [payloadClass: {}]", payloadClass);

        if (AuditIssuesFound.class.getName().equals(payloadClass)) {
            processAuditIssueFound(eventPacket.getPayloadContent());
        }
    }

    private void processAuditIssueFound(AuditIssuesFound event) {
        Map<String, Object> params = new HashMap<>();
        params.put("event", event);

        sendEmailTask.queueTask(event.getUserId(), TemplateName.AUDIT_ISSUE_FOUND, params);
        notificationService.createNotification(event.getUserId(), event.getDateDetected(),
            NotificationId.AUDIT_ISSUE_FOUND, params);
    }
}
