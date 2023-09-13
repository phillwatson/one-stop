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

import java.time.Duration;
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
    /**
     * The delay before messages polling begins.
     * TODO: move to a configurable property
     */
    private final static Duration INIT_DELAY = Duration.ofSeconds(10);

    /**
     * The frequency at which messages are polled from the database.
     * TODO: move to a configurable property
     */
    private final static Duration POLL_FREQUENCY = Duration.ofSeconds(5);

    /**
     * The maximum number of messages retrieved on each poll.
     * TODO: move to a configurable property
     */
    private final static int POLL_BATCH_SIZE = 25;

    // the repository to poll messages from the database
    private final EventRepository eventRepository;

    // the broker interface to send messages polled from the database
    private final Producer<String, EventPacket> producer;

    /**
     * A mutex to prevent delivery of events from multiple threads.
     */
    private final AtomicBoolean MUTEX = new AtomicBoolean();

    public void init(@Observes StartupEvent ev) {
        log.info("Scheduling EventDeliverer");
        ScheduledExecutorService executor = (ScheduledExecutorService)ExecutorFactory.newExecutor(ExecutorConfiguration.builder()
            .executorType(ExecutorType.SCHEDULED)
            .name("event-deliverer")
            .numberOfThreads(1)
            .build());

        executor.scheduleAtFixedRate(this::deliverEvents,
            INIT_DELAY.toSeconds(), POLL_FREQUENCY.toSeconds(), TimeUnit.SECONDS);
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
        log.trace("Polling events to deliver");
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
        List<EventEntity> events = eventRepository.listUndelivered(POLL_BATCH_SIZE);
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
