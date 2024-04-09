package com.hillayes.outbox.service;

import com.hillayes.commons.correlation.Correlation;
import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.executors.concurrent.ExecutorConfiguration;
import com.hillayes.executors.concurrent.ExecutorFactory;
import com.hillayes.executors.concurrent.ExecutorType;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import com.hillayes.outbox.repository.HospitalEntity;
import com.hillayes.outbox.repository.HospitalRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A scheduled service to read pending events from the event outbox table, at periodic
 * intervals, and send them to the message broker.
 * It also listens for events that have failed (raised errors during their processing)
 * and re-submit them.
 */
@ApplicationScoped
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
    private final static Duration POLL_FREQUENCY = Duration.ofSeconds(2);

    /**
     * The maximum number of messages retrieved on each poll.
     * TODO: move to a configurable property
     */
    private final static int POLL_BATCH_SIZE = 25;

    /**
     * The maximum number of times a failed event will be retried before being sent to the hospital.
     * TODO: move to a configurable property
     */
    private final static int MAX_RETRY_COUNT = 3;

    // the repository to poll messages from the database
    @Inject
    private EventRepository eventRepository;

    @Inject
    private HospitalRepository hospitalRepository;

    @Inject
    @TopicObserved(Topic.USER)
    private Event<EventPacket> userEvent;

    @Inject
    @TopicObserved(Topic.USER_AUTH)
    private Event<EventPacket> userAuthEvent;

    @Inject
    @TopicObserved(Topic.CONSENT)
    private Event<EventPacket> consentEvent;

    @Inject
    @TopicObserved(Topic.HOSPITAL_TOPIC)
    private Event<EventPacket> hospitalEvent;

    /**
     * A mutex to prevent delivery of events from multiple threads.
     */
    private final AtomicBoolean MUTEX = new AtomicBoolean();

    public void init(@Observes StartupEvent ev) {
        log.info("Scheduling EventDeliverer");
        ScheduledExecutorService executor = (ScheduledExecutorService) ExecutorFactory.newExecutor(ExecutorConfiguration.builder()
            .executorType(ExecutorType.SCHEDULED)
            .name("event-deliverer")
            .numberOfThreads(1)
            .build());

        executor.scheduleAtFixedRate(this::deliverEvents,
            INIT_DELAY.toSeconds(), POLL_FREQUENCY.toSeconds(), TimeUnit.SECONDS);
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

        events.forEach(entity -> {
            Event<EventPacket> sender = switch (entity.getTopic()) {
                case USER -> userEvent;
                case USER_AUTH -> userAuthEvent;
                case CONSENT -> consentEvent;
                case HOSPITAL_TOPIC -> hospitalEvent;
                default -> {
                    log.error("Unknown topic [topic: {}]", entity.getTopic());
                    throw new IllegalArgumentException("Unknown topic: " + entity.getTopic());
                }
            };

            EventPacket eventPacket = entity.toEventPacket();
            String prevCorrelation = Correlation.setCorrelationId(entity.getCorrelationId());
            try {
                // send event to consumers
                sender.fire(eventPacket);
            } catch (Exception e) {
                onError(eventPacket, e);
            } finally {
                Correlation.setCorrelationId(prevCorrelation);
            }

            // delete the entity
            eventRepository.delete(entity);
        });

        log.trace("Event delivery complete");
    }

    /**
     * Handles an error that occurred during the delivery of an event. The event will be
     * re-scheduled for delivery at a later time. If the event has reached the maximum
     * number of retries, it will be sent to the hospital.
     *
     * @param event the event that failed delivery.
     * @param error the error that occurred.
     */
    private void onError(EventPacket event, Throwable error) {
        log.error("Event delivery failed [id: {}, topic: {}, payload: {}]",
            event.getId(), event.getTopic(), event.getPayloadClass(), error);

        // has the event reached the max retry count
        int retryCount = event.getRetryCount();
        Topic failureTopic = (retryCount < MAX_RETRY_COUNT)
            ? event.getTopic()
            : Topic.HOSPITAL_TOPIC;

        if (log.isDebugEnabled()) {
            log.debug("Reposting event [failureTopic: {}, topic: {}, retryCount: {}]",
                failureTopic, event.getTopic(), event.getRetryCount());
        }

        // persist event for redelivery at a delayed time
        Instant scheduleFor = (failureTopic == Topic.HOSPITAL_TOPIC)
            ? Instant.now()
            : Instant.now().plusSeconds(20L * (retryCount + 1));
        eventRepository.save(EventEntity.forRedelivery(event, error, scheduleFor));

        if (failureTopic == Topic.HOSPITAL_TOPIC) {
            log.error("Writing failed event to message hospital [id: {}, topic: {}, retryCount: {}]",
                event.getId(), event.getTopic(), event.getRetryCount());

            // write the record to the event hospital table - with consumer and error
            hospitalRepository.save(HospitalEntity.fromEventPacket(event, null, error));
        }
    }
}
