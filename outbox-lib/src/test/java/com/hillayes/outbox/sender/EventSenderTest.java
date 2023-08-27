package com.hillayes.outbox.sender;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserAuthenticated;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EventSenderTest {
    private EventRepository eventRepository = mock(EventRepository.class);

    private EventSender fixture = new EventSender(eventRepository);

    @Test
    public void testSend() {
        // given: an event
        UserAuthenticated event = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // when: send is called
        fixture.send(Topic.USER, event);

        // then: the event is persisted
        ArgumentCaptor<EventEntity> argument = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).persist(argument.capture());

        // and: the event entity is created
        EventEntity eventEntity = argument.getValue();
        assertNull(eventEntity.getId());
        assertNotNull(eventEntity.getEventId());
        assertNotNull(eventEntity.getCorrelationId());
        assertEquals(Topic.USER, eventEntity.getTopic());
        assertEquals(UserAuthenticated.class.getName(), eventEntity.getPayloadClass());
        assertNotNull(eventEntity.getPayload());
        assertEquals(0, eventEntity.getRetryCount());
        assertNotNull(eventEntity.getScheduledFor());
        assertNotNull(eventEntity.getTimestamp());

        // and: the event has NO key
        assertNull(eventEntity.getKey());
    }

    @Test
    public void testSendWithKey() {
        // given: an event
        UserAuthenticated event = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // when: send is called
        fixture.send(Topic.USER, "key", event);

        // then: the event is persisted
        ArgumentCaptor<EventEntity> argument = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).persist(argument.capture());

        // and: the event entity is created
        EventEntity eventEntity = argument.getValue();
        assertNull(eventEntity.getId());
        assertNotNull(eventEntity.getEventId());
        assertNotNull(eventEntity.getCorrelationId());
        assertEquals(Topic.USER, eventEntity.getTopic());
        assertEquals(UserAuthenticated.class.getName(), eventEntity.getPayloadClass());
        assertNotNull(eventEntity.getPayload());
        assertEquals(0, eventEntity.getRetryCount());
        assertNotNull(eventEntity.getScheduledFor());
        assertNotNull(eventEntity.getTimestamp());

        // and: the event has a key
        assertEquals("key", eventEntity.getKey());
    }
}
