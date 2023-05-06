package com.hillayes.events.serializers;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hillayes.events.domain.EventPacket;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EventPacketDeserializer implements Deserializer<EventPacket> {
    private final JavaType type;
    private final ObjectMapper objectMapper;

    public EventPacketDeserializer() {
        this.type = TypeFactory.defaultInstance().constructType(EventPacket.class);
        this.objectMapper = MapperFactory.defaultMapper();
    }

    @Override
    public EventPacket deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (InputStream is = new ByteArrayInputStream(data)) {
            return objectMapper.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
