package com.hillayes.outbox.service;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import com.hillayes.outbox.repository.HospitalEntity;
import com.hillayes.outbox.repository.HospitalRepository;
import com.hillayes.events.sender.ProducerFactory;
import io.quarkus.scheduler.Scheduled;
import lombok.AllArgsConstructor;
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
import javax.transaction.Transactional;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A scheduled service to read pending events from the event outbox table, at periodic
 * intervals, and send them to the message broker.
 * It also listens for events that have failed (raised events during their processing)
 * and re-submit them.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class EventDeliverer {
    private final ProducerFactory producerFactory;
    private final EventRepository eventRepository;
    private final HospitalRepository hospitalRepository;

    private static final AtomicBoolean MUTEX = new AtomicBoolean();

    /**
     * A scheduled service to read pending events from the event outbox table and
     * send them to the message broker.
     */
    @Scheduled(cron = "${mensa.events.cron:0/2 * * * * ?}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void deliverEvents() throws Exception {
        if (!MUTEX.compareAndSet(false, true)) {
            return;
        }
        try {
            _deliverEvents();
        } finally {
            MUTEX.set(false);
        }
    }

    /**
     * Sends any undelivered events to the message broker, and marks them as delivered.
     */
    @Transactional(rollbackOn = Exception.class)
    protected void _deliverEvents() throws Exception {
        List<EventEntity> events = eventRepository.listUndelivered(25);
        log.trace("Found events for delivery [size: {}]", events.size());

        if (events.isEmpty()) {
            return;
        }
        log.trace("Event batch delivery started");

        Producer<String, EventPacket> producer = producerFactory.getProducer();
        List<EventTuple> records = events.stream()
            .map(entity -> {
                ProducerRecord<String, EventPacket> record =
                    new ProducerRecord<>(entity.getTopic().topicName(), entity.getKey(), entity.toEventPacket());

                return new EventTuple(entity, producer.send(record));
            })
            .toList();

        // wait for the events to be delivered
        for (EventTuple record : records) {
            try {
                // block until event delivery is complete
                record.eventResponse.get();

                // delete the entity
                eventRepository.delete(record.entity);
            } catch (InterruptedException | ExecutionException e) {
                EventEntity entity = record.entity;
                log.error("Event delivery failed [id: {}, topic: {}, payload: {}]",
                        entity.getId(), entity.getTopic(), entity.getPayloadClass());
                throw e;
            }
        }
        log.trace("Event delivery complete");
    }

    /**
     * Listens for events that have failed during their delivery. The event's retry-count
     * is incremented and, if the number of retries does not exceed the max, the event is
     * returned to the queue for delivery.
     *
     * @param record the record of the failed event.
     */
    @Incoming("dead-letter-topic")
    @Transactional
    public void deadLetterTopicListener(ConsumerRecord<String, EventPacket> record) {
        Headers headers = record.headers();
        String reason = getHeader(headers, "dead-letter-reason");
        String cause = getHeader(headers, "dead-letter-cause");

        EventPacket event = record.value();
        log.debug("Event failed processing [topic: {}, retryCount: {}, reason: {}, cause: {}]",
            event.getTopic(), event.getRetryCount(), reason, cause);

        if (event.getRetryCount() <= 3) {
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

    @AllArgsConstructor
    private static class EventTuple {
        private EventEntity entity;
        private Future<RecordMetadata> eventResponse;
    }
}
