package com.hillayes.events.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static com.hillayes.events.consumer.HeadersUtils.*;

@RequiredArgsConstructor
@Slf4j
public class ConsumerErrorHandler {
    private final Producer<String, EventPacket> producer;

    public void handle(ConsumerRecord<String, EventPacket> record, Throwable error) {
        EventPacket eventPacket = record.value();

        int retryCount = eventPacket.getRetryCount();
        Topic failureTopic = (retryCount < 3)
            ? Topic.RETRY_TOPIC
            : Topic.HOSPITAL_TOPIC;

        if (log.isDebugEnabled()) {
            log.debug("Reposting event [failureTopic: {}, topic: {}, retryCount: {}, cause: {}]",
                failureTopic, eventPacket.getTopic(), eventPacket.getRetryCount(), error.getMessage());
        }

        ProducerRecord<String, EventPacket> retryRecord =
            new ProducerRecord<>(failureTopic.topicName(), eventPacket);

        retryRecord.headers()
            .add(REASON_HEADER, error.getClass().getName().getBytes(StandardCharsets.UTF_8))
            .add(CAUSE_HEADER, error.getMessage().getBytes(StandardCharsets.UTF_8));

        if (failureTopic == Topic.RETRY_TOPIC) {
            // calculate a time to retry the event
            Instant scheduleFor = Instant.now().plusSeconds(20L * (retryCount + 1));
            retryRecord.headers()
                .add(SCHEDULE_HEADER, scheduleFor.toString().getBytes(StandardCharsets.UTF_8));
        }

        // send asynchronously to chosen failure topic
        producer.send(retryRecord, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error sending event to topic [topic: {}, eventId: {}, correlationId: {}]",
                    record.topic(), eventPacket.getId(), eventPacket.getCorrelationId(), exception);
            } else {
                log.trace("Event sent to topic [topic: {}, event: {}]", record.topic(), eventPacket.getTopic());
            }
        });
    }
}
