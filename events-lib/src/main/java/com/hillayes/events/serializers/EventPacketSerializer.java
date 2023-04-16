package com.hillayes.events.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.events.domain.EventPacket;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

public class EventPacketSerializer extends ObjectMapperSerializer<EventPacket> {
    public EventPacketSerializer() {
        super();
    }
}
