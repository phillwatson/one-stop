package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.annotation.TopicObserver;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.task.SendEmailTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumes hospital-topic events, which are events that have failed delivery and are not
 * to be retried. An email will be sent to the system administrator.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class HospitalTopicConsumer {
    private final SendEmailTask sendEmailTask;

    /**
     * Listens for events that have failed during their delivery. The event details
     * will be sent to the system administrator via email.
     *
     * @param event the failed event.
     */
    @TopicObserver
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void consume(@Observes
                        @TopicObserved(Topic.HOSPITAL_TOPIC) EventPacket event) {
        log.info("Received event hospital event [payloadClass: {}]", event.getPayloadClass());

        Map<String, Object> params = new HashMap<>();
        params.put("event", event);
        params.put("reason", event.getReason());
        params.put("cause", event.getCause());
        sendEmailTask.queueTask(TemplateName.EVENT_HOSPITAL, params);
    }
}
