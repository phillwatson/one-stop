package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.task.SendEmailTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;

import java.util.HashMap;
import java.util.Map;

import static com.hillayes.events.consumer.HeadersUtils.*;

/**
 * Consumes hospital-topic events, which are events that have failed delivery and are not
 * to be retried. An email will be sent to the system administrator.
 */
@ApplicationScoped
@TopicConsumer(Topic.HOSPITAL_TOPIC)
@RequiredArgsConstructor
@Slf4j
public class HospitalTopicConsumer implements EventConsumer {
    private final SendEmailTask sendEmailTask;

    /**
     * Listens for events that have failed during their delivery. The event details
     * will be sent to the system administrator via email.
     *
     * @param record the record containing the failed event.
     */
    @Transactional
    public void consume(ConsumerRecord<String, EventPacket> record) {
        EventPacket event = record.value();
        log.info("Received event hospital event [payloadClass: {}]", event.getPayloadClass());

        Headers headers = record.headers();
        String reason = getHeader(headers, REASON_HEADER).orElse(null);
        String cause = getHeader(headers, CAUSE_HEADER).orElse(null);

        Map<String, Object> params = new HashMap<>();
        params.put("event", event);
        params.put("reason", reason);
        params.put("cause", cause);
        sendEmailTask.queueJob(TemplateName.EVENT_HOSPITAL, params);
    }
}
