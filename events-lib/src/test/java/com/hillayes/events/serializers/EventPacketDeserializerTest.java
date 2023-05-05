package com.hillayes.events.serializers;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserLogin;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EventPacketDeserializerTest {
    private final EventPacketSerializer serializer = new EventPacketSerializer();

    private final EventPacketDeserializer fixture = new EventPacketDeserializer();

    @Test
    public void testDeserialize() throws Exception {
        // given: an event payload
        UserLogin payload = UserLogin.builder()
            .userId(UUID.randomUUID())
            .dateLogin(Instant.now())
            .build();

        // and: an EventPacket
        EventPacket eventPacket = new EventPacket(
            UUID.randomUUID(),
            Topic.USER_AUTH,
            UUID.randomUUID().toString(),
            0, Instant.now(),
            "key", payload.getClass().getName(), EventPacket.serialize(payload)
        );

        // and: the event packet is serialized
        byte[] serialized = serializer.serialize(eventPacket.getTopic().topicName(), eventPacket);

        // when: the event packet is deserialized
        EventPacket deserialized = fixture.deserialize(eventPacket.getTopic().topicName(), serialized);

        // then: the deserialized event packet is not null
        assertNotNull(deserialized);

        // and: the deserialized event packet is equal to the original event packet
        assertEquals(eventPacket, deserialized);

        // and: the deserialized event packet has the same properties as the original event packet
        assertEquals(eventPacket.getId(), deserialized.getId());
        assertEquals(eventPacket.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals(eventPacket.getTopic(), deserialized.getTopic());
        assertEquals(eventPacket.getRetryCount(), deserialized.getRetryCount());
        assertEquals(eventPacket.getTimestamp(), deserialized.getTimestamp());
        assertEquals(eventPacket.getKey(), deserialized.getKey());
        assertEquals(eventPacket.getPayloadClass(), deserialized.getPayloadClass());
        assertEquals(eventPacket.getPayload(), deserialized.getPayload());
    }

    @Test
    public void testDeerializeNull() throws Exception {
        // when: the event packet is serialized
        EventPacket deserialized = fixture.deserialize(Topic.USER_AUTH.topicName(), null);

        // then: the deserialized event packet is null
        assertNull(deserialized);
    }
}
