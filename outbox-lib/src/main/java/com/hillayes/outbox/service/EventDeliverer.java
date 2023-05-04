package com.hillayes.outbox.service;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import io.quarkus.scheduler.Scheduled;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A scheduled service to read pending events from the event outbox table, at periodic
 * intervals, and send them to the message broker.
 * It also listens for events that have failed (raised errors during their processing)
 * and re-submit them.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class EventDeliverer {
    private final Producer<String, EventPacket> producer;
    private final EventRepository eventRepository;

    /**
     * A mutex to prevent delivery of events from multiple threads.
     */
    private static final AtomicBoolean MUTEX = new AtomicBoolean();

    /**
     * Ensures that the producer is closed when the application is stopped.
     */
    @PreDestroy
    public void onStop() {
        log.info("Shutting down EventDeliverer - started");
        if (producer != null) {
            log.info("Closing producer {}", producer);
            producer.close();
        }
        log.info("Shutting down EventDeliverer - complete");
    }

    /**
     * A scheduled service to read pending events from the event outbox table and
     * send them to the message broker.
     */
    @Scheduled(cron = "${mensa.events.cron:0/2 * * * * ?}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void deliverEvents() throws Exception {
        // if previous delivery is still in progress, skip this run
        if (MUTEX.compareAndSet(false, true)) {
            try {
                _deliverEvents();
            } catch (Exception ignored) {
            } finally {
                MUTEX.set(false);
            }
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
            } catch (ExecutionException e) {
                EventEntity entity = record.entity;
                log.error("Event delivery failed [id: {}, topic: {}, payload: {}]",
                        entity.getId(), entity.getTopic(), entity.getPayloadClass(), e.getCause());
                throw e;
            }
        }
        log.trace("Event delivery complete");
    }


    @AllArgsConstructor
    private static class EventTuple {
        private EventEntity entity;
        private Future<RecordMetadata> eventResponse;
    }
}
