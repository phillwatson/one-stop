package com.hillayes.outbox.service;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserLogin;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import io.quarkus.runtime.ShutdownEvent;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventDelivererTest {
    private Producer<String, EventPacket> producer = mock(Producer.class);
    private EventRepository eventRepository = mock(EventRepository.class);

    private EventDeliverer fixture = new EventDeliverer(producer, eventRepository);

    @BeforeEach
    public void beforeEach() {
        reset(producer, eventRepository);

        when(producer.send(any())).then(invocationOnMock -> mockResult());
    }

    @Test
    public void testStop() {
        // when: the fixture is stopped
        fixture.onStop(new ShutdownEvent());

        // then: the producer is closed
        verify(producer).close();
    }

    @Test
    public void testDeliverEvents_noEvents() throws Exception {
        // given: no waiting events
        when(eventRepository.listUndelivered(anyInt())).thenReturn(Collections.emptyList());

        // when: events are delivered
        fixture.deliverEvents();

        // then: waiting events are retrieved from the repository
        verify(eventRepository).listUndelivered(anyInt());
    }

    @Test
    public void testDeliverEvents() throws Exception {
        // given: waiting events
        List<EventEntity> waitingEvents = List.of(
            createEventEntity(),
            createEventEntity(),
            createEventEntity()
        );
        when(eventRepository.listUndelivered(anyInt())).thenReturn(waitingEvents);

        // when: events are delivered
        fixture.deliverEvents();

        // then: waiting events are retrieved from the repository
        verify(eventRepository).listUndelivered(anyInt());

        // and: events are all sent to the broker
        verify(producer, times(waitingEvents.size())).send(any());

        // and: the events are deleted from the repository
        verify(eventRepository, times(waitingEvents.size())).delete(any());
    }

    private EventEntity createEventEntity() {
        // given: an event payload
        UserLogin event = UserLogin.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // and: an event entity is created
        return EventEntity.forInitialDelivery(Topic.USER_AUTH, "test-key", event);
    }

    private Future<RecordMetadata> mockResult() {
        return new Future<>() {
            @Override
            public RecordMetadata get() {
                return get(100, TimeUnit.MILLISECONDS);
            }

            @Override
            public RecordMetadata get(long timeout, TimeUnit unit) {
                try {
                    Thread.sleep(500);
                    return mockRecordMetadata();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }
        };
    }

    private RecordMetadata mockRecordMetadata() {
        return new RecordMetadata(
            new TopicPartition("test-topic", 0),
            0,
            0,
            0,
            0L,
            0,
            0
        );
    }
}
