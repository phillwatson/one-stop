package com.hillayes.outbox.service;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.executors.concurrent.ExecutorConfiguration;
import com.hillayes.executors.concurrent.ExecutorFactory;
import com.hillayes.executors.concurrent.ExecutorType;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A scheduled service to read pending events from the event outbox table, at periodic
 * intervals, and send them to the message broker.
 * It also listens for events that have failed (raised errors during their processing)
 * and re-submit them.
 */
@Dependent
@RequiredArgsConstructor
@Slf4j
public class EventDeliverer {
    private final Producer<String, EventPacket> producer;
    private final EventRepository eventRepository;

    /**
     * A mutex to prevent delivery of events from multiple threads.
     */
    private static final AtomicBoolean MUTEX = new AtomicBoolean();

    public void init(@Observes StartupEvent ev) {
        log.info("Scheduling EventDeliverer");
        ScheduledExecutorService executor = (ScheduledExecutorService)ExecutorFactory.newExecutor(ExecutorConfiguration.builder()
            .executorType(ExecutorType.SCHEDULED)
            .name("event-deliverer")
            .numberOfThreads(1)
            .build());

        executor.scheduleAtFixedRate(this::deliverEvents, 10, 60, TimeUnit.SECONDS);
    }

    /**
     * Ensures that the producer is closed when the application is stopped.
     */
    public void onStop(@Observes ShutdownEvent ev) {
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
    public void deliverEvents() {
        log.debug("Polling events to deliver");
        // if previous delivery is still in progress, skip this run
        if (MUTEX.compareAndSet(false, true)) {
            log.trace("Event delivery passed mutex");
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
                log.error("Event delivery failed will be retried [id: {}, topic: {}, payload: {}]",
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
