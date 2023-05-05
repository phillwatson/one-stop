package com.hillayes.events.serializers;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.UserLogin;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EventPacketSerializerTest {
    private final EventPacketSerializer fixture = new EventPacketSerializer();

    @Test
    public void testSerialize() throws Exception {
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

        // when: the event packet is serialized
        byte[] serialized = fixture.serialize(eventPacket.getTopic().topicName(), eventPacket);

        // then: the serialized event packet is not null
        assertNotNull(serialized);
    }

    @Test
    public void testSerializeNull() throws Exception {
        // when: the event packet is serialized
        byte[] serialized = fixture.serialize(Topic.USER_AUTH.topicName(), null);

        // then: the serialized event packet is null
        assertNull(serialized);
    }
}
