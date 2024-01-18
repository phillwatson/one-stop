package com.hillayes.outbox.service;

import com.hillayes.events.annotation.ConsumerGroup;
import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.outbox.repository.HospitalEntity;
import com.hillayes.outbox.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import static com.hillayes.events.consumer.HeadersUtils.*;

/**
 * Consumes hospital-topic events, which are events that have failed delivery and are not
 * to be retried.
 *
 * All services that use the outbox library will have a hospital-topic consumer. By setting
 * them all with the same consumer group, we can ensure that only one service will attempt
 * to process a given failed event.
 */
@ApplicationScoped
@TopicConsumer(Topic.HOSPITAL_TOPIC)
@ConsumerGroup("hospital-topic-group")
@RequiredArgsConstructor
@Slf4j
public class HospitalTopicConsumer implements EventConsumer {
    private final HospitalRepository hospitalRepository;

    /**
     * Listens for events that have failed during their delivery. The event will be written
     * to the message hospital table.
     *
     * @param record the record containing the failed event.
     */
    @Transactional
    public void consume(ConsumerRecord<String, EventPacket> record) {
        Headers headers = record.headers();
        String reason = getHeader(headers, REASON_HEADER).orElse(null);
        String cause = getHeader(headers, CAUSE_HEADER).orElse(null);

        EventPacket event = record.value();

        log.error("Writing failed event to message hospital [id: {}, topic: {}, retryCount: {}, reason: {}, cause: {}]",
            event.getId(), event.getTopic(), event.getRetryCount(), reason, cause);

        // write the record to the event hospital table - with reason and cause
        hospitalRepository.save(HospitalEntity.fromEventPacket(event, reason, cause));
    }
}
