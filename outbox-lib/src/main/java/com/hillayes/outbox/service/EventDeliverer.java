package com.hillayes.outbox.service;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import com.hillayes.outbox.sender.ProducerFactory;
import io.quarkus.scheduler.Scheduled;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class EventDeliverer {
    private final ProducerFactory producerFactory;
    private final EventRepository eventRepository;

    private static final AtomicBoolean MUTEX = new AtomicBoolean();

    /**
     * A scheduled service to read pending events from the event outbox table and
     * send them to the message broker.
     */
    @Scheduled(cron = "${mensa.events.cron:1/5 * * * * ?}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void deliverEvents() throws Exception {
        if (!MUTEX.compareAndSet(false, true)) {
            return;
        }
        try {
            _deliverEvents();
        } finally {
            log.trace("Event delivery complete");
            MUTEX.set(false);
        }
    }

    /**
     * Sends any undelivered events to the message broker, and marks them as delivered.
     */
    @Transactional(rollbackOn = Exception.class)
    protected void _deliverEvents() throws Exception {
        log.trace("Event delivery started");
        List<EventEntity> events = eventRepository.listUndelivered(25);
        log.trace("Found events for delivery [size: {}]", events.size());

        if (events.isEmpty()) {
            return;
        }

        Producer<String, EventPacket> producer = producerFactory.getProducer();
        List<EventRecord> records = events.stream()
            .map(event -> send(producer, event)).toList();

        for (EventRecord record : records) {
            try {
                // block until event delivery is complete
                record.getEventResponse().get();

                // record the delivery time
                EventEntity event = record.getEvent();
                event.setDeliveredAt(Instant.now());
                eventRepository.persist(event);
            } catch (InterruptedException | ExecutionException e) {
                EventEntity event = record.getEvent();
                log.error("Event delivery failed [id: {}, topic: {}, payload: {}]",
                        event.getId(), event.getTopic(), event.getPayloadClass());
                throw e;
            }
        }
    }

    /**
     * Listens for events that have failed during their delivery. The event's retry-count
     * is incremented and, if the number of retries does not exceed the max, the event is
     * returned to the queue for delivery.
     * @param record the record of the failed event.
     */
    @Incoming("retry-topic")
    @Transactional
    public void deadLetterTopicListener(ConsumerRecord<String, EventPacket> record) {
        Headers headers = record.headers();
        String reason = getHeader(headers, "dead-letter-reason");
        String cause = getHeader(headers, "dead-letter-cause");

        EventPacket event = record.value();
        int retryCount = event.getRetryCount();

        if (retryCount < 3) {
            log.debug("Reposting event [topic: {}, retryCount: {}, reason: {}, cause: {}]",
                    event.getTopic(), retryCount, reason, cause);

            EventEntity entity = eventRepository.findById(event.getId(), LockModeType.PESSIMISTIC_WRITE);
            entity.setRetryCount(retryCount + 1);
            entity.setDeliveredAt(null);
            eventRepository.persist(entity);
        } else {
            log.error("Failed to deliver event [id: {}, topic: {}, retryCount: {}, reason: {}, cause: {}]",
                    event.getId(), event.getTopic(), retryCount, reason, cause);
        }
    }

    private EventRecord send(Producer<String, EventPacket> producer, EventEntity event) {
        ProducerRecord<String, EventPacket> record = new ProducerRecord<>(event.getTopic().topicName(), event.toEntityPacket());
        return EventRecord.builder()
                .event(event)
                .eventResponse(producer.send(record))
                .build();
    }

    private String getHeader(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        return (header == null) ? null:new String(header.value());
    }

    @Builder
    @Getter
    private static class EventRecord {
        private EventEntity event;
        private Future<RecordMetadata> eventResponse;
    }
}
