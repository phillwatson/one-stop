package com.hillayes.events.consumer;

import com.hillayes.events.domain.EventPacket;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface EventConsumer {
    default public void consume(EventPacket eventPacket) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    default public void consume(ConsumerRecord<String, EventPacket> record) throws Exception {
        consume(record.value());
    }
}
