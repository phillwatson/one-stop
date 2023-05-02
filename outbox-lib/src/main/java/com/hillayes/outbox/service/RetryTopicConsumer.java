package com.hillayes.outbox.service;

import com.hillayes.events.annotation.ConsumerGroup;
import com.hillayes.events.annotation.ConsumerTopic;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import com.hillayes.outbox.repository.HospitalEntity;
import com.hillayes.outbox.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

/**
 * Consumes retry-topic events, which are events that have failed delivery. All
 * services that use the outbox library will have a retry-topic consumer. By setting
 * them all with the same consumer group, we can ensure that only one service will
 * attempt to process a given failed event.
 */
@ApplicationScoped
@ConsumerTopic(Topic.RETRY_TOPIC)
@ConsumerGroup("retry-topic-group")
@RequiredArgsConstructor
@Slf4j
public class RetryTopicConsumer implements EventConsumer {
    private final EventRepository eventRepository;
    private final HospitalRepository hospitalRepository;

    /**
     * Listens for events that have failed during their delivery. The event's retry-count
     * is incremented and, if the number of retries does not exceed the max, the event is
     * returned to the queue for delivery.
     *
     * @param record the record of the failed event.
     */
    @Transactional
    public void consume(ConsumerRecord<String, EventPacket> record) {
        Headers headers = record.headers();
        String reason = getHeader(headers, "dead-letter-reason");
        String cause = getHeader(headers, "dead-letter-cause");

        EventPacket event = record.value();
        if (log.isDebugEnabled()) {
            log.debug("Event failed processing [topic: {}, retryCount: {}, reason: {}, cause: {}]",
                event.getTopic(), event.getRetryCount(), reason, cause);
        }

        if (event.getRetryCount() < 3) {
            if (log.isDebugEnabled()) {
                log.debug("Reposting event [topic: {}, retryCount: {}, reason: {}, cause: {}]",
                    event.getTopic(), event.getRetryCount(), reason, cause);
            }

            EventEntity entity = EventEntity.forRedelivery(event);
            eventRepository.persist(entity);
        } else {
            log.error("Failed to deliver event [id: {}, topic: {}, retryCount: {}, reason: {}, cause: {}]",
                event.getId(), event.getTopic(), event.getRetryCount(), reason, cause);

            // write the record to the event hospital table - with reason and cause
            hospitalRepository.persist(HospitalEntity.fromEventPacket(event, reason, cause));
        }
    }

    private String getHeader(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        return (header == null) ? null:new String(header.value());
    }
}
