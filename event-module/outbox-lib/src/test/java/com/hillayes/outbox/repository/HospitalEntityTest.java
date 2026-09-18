package com.hillayes.outbox.repository;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserAuthenticated;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HospitalEntityTest {
    @Test
    public void testFromEventEntity() {
        // given: an event payload
        UserAuthenticated payload = UserAuthenticated.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // and: an event packet to serialize the payload
        EventEntity eventEntity = EventEntity.forInitialDelivery(Topic.USER_AUTH, "key", payload);

        // when: the event packet is converted to a hospital entity
        Throwable error = new RuntimeException("mock cause");
        HospitalEntity hospitalEntity = HospitalEntity.fromEventEntity(eventEntity,  "mock consumer", error);

        // then: the hospital entity is created with correct values
        assertNotNull(hospitalEntity);
        assertNull(hospitalEntity.getId());
        assertNotNull(hospitalEntity.getTimestamp());
        assertEquals(eventEntity.getId(), hospitalEntity.getEventId());
        assertEquals(eventEntity.getCorrelationId(), hospitalEntity.getCorrelationId());
        assertEquals(eventEntity.getKey(), hospitalEntity.getKey());
        assertEquals(eventEntity.getTopic(), hospitalEntity.getTopic());
        assertEquals(eventEntity.getRetryCount(), hospitalEntity.getRetryCount());
        assertEquals(eventEntity.getPayloadClass(), hospitalEntity.getPayloadClass());
        assertEquals(eventEntity.getPayload(), hospitalEntity.getPayload());
        assertEquals("mock consumer", hospitalEntity.getConsumer());
        assertEquals(RuntimeException.class.getName(), hospitalEntity.getReason());
        assertEquals("mock cause", hospitalEntity.getCause());
    }
}
