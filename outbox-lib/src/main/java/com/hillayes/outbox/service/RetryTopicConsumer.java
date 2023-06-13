package com.hillayes.outbox.service;

import com.hillayes.events.annotation.ConsumerGroup;
import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;

import static com.hillayes.events.consumer.HeadersUtils.*;

/**
 * Consumes retry-topic events, which are events that have failed delivery.
 *
 * All services that use the outbox library will have a retry-topic consumer. By setting
 * them all with the same consumer group, we can ensure that only one service will attempt
 * to process a given failed event.
 */
@ApplicationScoped
@TopicConsumer(Topic.RETRY_TOPIC)
@ConsumerGroup("retry-topic-group")
@RequiredArgsConstructor
@Slf4j
public class RetryTopicConsumer implements EventConsumer {
    public static final Duration DEFAULT_RESCHEDULE_OFFSET = Duration.ofSeconds(60);

    private final EventRepository eventRepository;

    /**
     * Listens for events that have failed during their delivery. The event's retry-count
     * is incremented and, if the number of retries does not exceed the max, the event is
     * returned to the queue for delivery.
     *
     * @param record the record containing the failed event.
     */
    @Transactional
    public void consume(ConsumerRecord<String, EventPacket> record) {
        Headers headers = record.headers();
        String reason = getHeader(headers, REASON_HEADER).orElse(null);
        String cause = getHeader(headers, CAUSE_HEADER).orElse(null);

        EventPacket event = record.value();
        if (log.isDebugEnabled()) {
            log.debug("Retrying failed event [topic: {}, retryCount: {}, reason: {}, cause: {}]",
                event.getTopic(), event.getRetryCount(), reason, cause);
        }

        // determine when to retry the event - error handler may have set a schedule-for header
        Instant scheduleFor = getHeader(headers, SCHEDULE_HEADER)
            .map(Instant::parse)
            .orElse(Instant.now().plus(DEFAULT_RESCHEDULE_OFFSET));

        EventEntity entity = EventEntity.forRedelivery(event, scheduleFor);
        eventRepository.persist(entity);
    }
}
