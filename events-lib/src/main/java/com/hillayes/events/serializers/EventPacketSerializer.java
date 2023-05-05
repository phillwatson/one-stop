package com.hillayes.events.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.events.domain.EventPacket;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EventPacketSerializer implements Serializer<EventPacket> {
    private final ObjectMapper objectMapper;

    public EventPacketSerializer() {
        this.objectMapper = MapperFactory.defaultMapper();
    }

    @Override
    public byte[] serialize(String topic, EventPacket data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectMapper.writeValue(output, data);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
