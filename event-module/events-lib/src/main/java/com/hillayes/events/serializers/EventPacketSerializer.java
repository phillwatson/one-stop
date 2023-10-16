package com.hillayes.events.serializers;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.events.domain.EventPacket;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EventPacketSerializer implements Serializer<EventPacket> {
    private final ObjectWriter objectWriter;

    public EventPacketSerializer() {
        this.objectWriter = MapperFactory.writerFor(EventPacket.class);
    }

    @Override
    public byte[] serialize(String topic, EventPacket data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectWriter.writeValue(output, data);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
