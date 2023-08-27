package com.hillayes.events.domain;

import com.hillayes.events.events.auth.UserAuthenticated;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventPacketTest {
    @Test
    public void testConstructor() throws Exception {
        // given: an event payload
        UserAuthenticated payload = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // and: an event ID
        UUID eventId = UUID.randomUUID();

        // and: a correlation ID
        String correlationId = UUID.randomUUID().toString();

        // and: a timestamp
        Instant timestamp = Instant.now();

        // and: a payload
        String payloadStr = EventPacket.serialize(payload);

        // when: an EventPacket is constructed
        EventPacket eventPacket = new EventPacket(
            eventId, Topic.USER_AUTH, correlationId, 0, timestamp,
            "key", payload.getClass().getName(), payloadStr
        );

        // then: the event packet contains the correct properties
        assertEquals(eventId, eventPacket.getId());
        assertEquals(Topic.USER_AUTH, eventPacket.getTopic());
        assertEquals(correlationId, eventPacket.getCorrelationId());
        assertEquals(0, eventPacket.getRetryCount());
        assertEquals(timestamp, eventPacket.getTimestamp());
        assertEquals("key", eventPacket.getKey());
        assertEquals(payload.getClass().getName(), eventPacket.getPayloadClass());
        assertEquals(payloadStr, eventPacket.getPayload());
    }

    @Test
    public void testGetPayloadContent() throws Exception {
        // given: an event payload
        UserAuthenticated payload = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // and: an event ID
        UUID eventId = UUID.randomUUID();

        // and: a correlation ID
        String correlationId = UUID.randomUUID().toString();

        // and: a timestamp
        Instant timestamp = Instant.now();

        // and: a payload
        String payloadStr = EventPacket.serialize(payload);

        // and: an EventPacket is constructed
        EventPacket eventPacket = new EventPacket(
            eventId, Topic.USER_AUTH, correlationId, 0, timestamp,
            "key", payload.getClass().getName(), payloadStr
        );

        // when: the payload content is retrieved
        UserAuthenticated payloadContent = eventPacket.getPayloadContent();

        // then: the event packet contains the correct properties
        assertEquals(payload.getUserId(), payloadContent.getUserId());
        assertEquals(payload.getDateLogin(), payloadContent.getDateLogin());
    }
}
