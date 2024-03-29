package com.hillayes.outbox.repository;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserAuthenticated;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HospitalEntityTest {
    @Test
    public void testFromEventPacket() {
        // given: an event payload
        UserAuthenticated payload = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // and: an event packet to serialize the payload
        EventPacket eventPacket = new EventPacket(
            UUID.randomUUID(),
            Topic.USER_AUTH,
            UUID.randomUUID().toString(),
            0, Instant.now(),
            "key", payload.getClass().getName(), EventPacket.serialize(payload)
        );

        // when: the event packet is converted to a hospital entity
        HospitalEntity hospitalEntity = HospitalEntity.fromEventPacket(eventPacket,  "mock consumer", "mock reason", "mock cause");

        // then: the hospital entity is created with correct values
        assertNotNull(hospitalEntity);
        assertNull(hospitalEntity.getId());
        assertNotNull(hospitalEntity.getTimestamp());
        assertEquals(eventPacket.getId(), hospitalEntity.getEventId());
        assertEquals(eventPacket.getCorrelationId(), hospitalEntity.getCorrelationId());
        assertEquals(eventPacket.getKey(), hospitalEntity.getKey());
        assertEquals(eventPacket.getTopic(), hospitalEntity.getTopic());
        assertEquals(eventPacket.getRetryCount(), hospitalEntity.getRetryCount());
        assertEquals(eventPacket.getPayloadClass(), hospitalEntity.getPayloadClass());
        assertEquals(eventPacket.getPayload(), hospitalEntity.getPayload());
        assertEquals("mock consumer", hospitalEntity.getConsumer());
        assertEquals("mock reason", hospitalEntity.getReason());
        assertEquals("mock cause", hospitalEntity.getCause());
    }
}
