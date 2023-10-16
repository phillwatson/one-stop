package com.hillayes.events.serializers;

import com.fasterxml.jackson.databind.ObjectReader;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.events.domain.EventPacket;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EventPacketDeserializer implements Deserializer<EventPacket> {
    private final ObjectReader objectReader;

    public EventPacketDeserializer() {
        this.objectReader = MapperFactory.readerFor(EventPacket.class);
    }

    @Override
    public EventPacket deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (InputStream is = new ByteArrayInputStream(data)) {
            return objectReader.readValue(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
