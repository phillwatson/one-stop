package com.hillayes.outbox.service;

import com.hillayes.events.consumer.ConsumerTopic;
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

@ApplicationScoped
@ConsumerTopic(topic = Topic.RETRY_TOPIC)
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
        log.debug("Event failed processing [topic: {}, retryCount: {}, reason: {}, cause: {}]",
            event.getTopic(), event.getRetryCount(), reason, cause);

        if (event.getRetryCount() < 3) {
            log.debug("Reposting event [topic: {}, retryCount: {}, reason: {}, cause: {}]",
                event.getTopic(), event.getRetryCount(), reason, cause);

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
