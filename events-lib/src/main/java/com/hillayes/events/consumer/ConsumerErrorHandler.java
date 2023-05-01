package com.hillayes.events.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Slf4j
public class ConsumerErrorHandler {
    private final Producer<String, EventPacket> producer;

    public void handle(ConsumerRecord<String, EventPacket> record, Throwable error) {
        EventPacket eventPacket = record.value();
        ProducerRecord<String, EventPacket> retryRecord =
            new ProducerRecord<>(Topic.RETRY_TOPIC.topicName(), eventPacket);
        retryRecord.headers().add("dead-letter-reason", "error".getBytes(StandardCharsets.UTF_8));
        retryRecord.headers().add("dead-letter-cause", error.getMessage().getBytes(StandardCharsets.UTF_8));

        producer.send(retryRecord, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error sending event to topic [topic: {}, event: {}]", record.topic(), eventPacket.getTopic());
            } else {
                log.debug("Event sent to topic [topic: {}, event: {}]", record.topic(), eventPacket.getTopic());
            }
        });
    }
}
