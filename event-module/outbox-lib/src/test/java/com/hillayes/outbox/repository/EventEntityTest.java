package com.hillayes.outbox.repository;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserAuthenticated;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EventEntityTest {
    @Test
    public void testInitialDelivery() {
        // given: an event payload
        UserAuthenticated event = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // when: an event entity is created
        EventEntity eventEntity = EventEntity.forInitialDelivery(Topic.USER_AUTH, "test-key", event);

        // then: the event entity is created
        assertNull(eventEntity.getId());
        assertNotNull(eventEntity.getEventId());
        assertNotNull(eventEntity.getCorrelationId());
        assertEquals(Topic.USER_AUTH, eventEntity.getTopic());
        assertEquals("test-key", eventEntity.getKey());
        assertEquals(UserAuthenticated.class.getName(), eventEntity.getPayloadClass());
        assertNotNull(eventEntity.getPayload());
        assertEquals(0, eventEntity.getRetryCount());
        assertNotNull(eventEntity.getScheduledFor());
        assertEquals(eventEntity.getTimestamp(), eventEntity.getScheduledFor());
    }

    @Test
    public void testRedelivery() {
        // given: an event payload
        UserAuthenticated event = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // and: an event entity is created
        EventEntity eventEntity = EventEntity.forInitialDelivery(Topic.USER_AUTH, "test-key", event);

        // and: the event entity is converted to a packet
        EventPacket eventPacket = eventEntity.toEventPacket();

        // when: the event entity is to be redelivered
        Instant redeliveryTime = Instant.now().plusSeconds(60);
        EventEntity redelivery = EventEntity.forRedelivery(eventPacket, redeliveryTime);

        // then: the redelivery event entity matches the original
        assertNull(eventEntity.getId());
        assertEquals(eventEntity.getEventId(), redelivery.getEventId());
        assertEquals(eventEntity.getCorrelationId(), redelivery.getCorrelationId());
        assertEquals(eventEntity.getTopic(), redelivery.getTopic());
        assertEquals(eventEntity.getKey(), redelivery.getKey());
        assertEquals(eventEntity.getPayloadClass(), redelivery.getPayloadClass());
        assertEquals(eventEntity.getPayload(), redelivery.getPayload());
        assertEquals(redeliveryTime, redelivery.getScheduledFor());
        assertEquals(eventEntity.getTimestamp(), eventEntity.getTimestamp());

        // and: the retry count is incremented
        assertEquals(eventEntity.getRetryCount() + 1, redelivery.getRetryCount());
    }

    @Test
    public void testToEventPacket() {
        // given: an event payload
        UserAuthenticated event = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // and: an event entity is created
        EventEntity eventEntity = EventEntity.forInitialDelivery(Topic.USER_AUTH, "test-key", event);

        // when: the event entity is converted to a packet
        EventPacket eventPacket = eventEntity.toEventPacket();

        // then: the event packet matches the event entity matches
        assertNull(eventEntity.getId());
        assertEquals(eventEntity.getEventId(), eventPacket.getId());
        assertEquals(eventEntity.getCorrelationId(), eventPacket.getCorrelationId());
        assertEquals(eventEntity.getTopic(), eventPacket.getTopic());
        assertEquals(eventEntity.getKey(), eventPacket.getKey());
        assertEquals(eventEntity.getPayloadClass(), eventPacket.getPayloadClass());
        assertEquals(eventEntity.getPayload(), eventPacket.getPayload());
        assertEquals(eventEntity.getTimestamp(), eventPacket.getTimestamp());
        assertEquals(eventEntity.getRetryCount(), eventPacket.getRetryCount());
    }
}
