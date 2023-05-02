package com.hillayes.audit.event.consumer;

import com.hillayes.events.annotation.ConsumerTopic;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.executors.correlation.Correlation;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ConsumerTopic(Topic.CONSENT)

@Slf4j
public class ConsentTopicConsumer implements EventConsumer {
    public void consume(EventPacket eventPacket) {
        Correlation.setCorrelationId(eventPacket.getCorrelationId());
        try {
            log.info("Received consent event [payloadClass: {}]", eventPacket.getPayloadClass());
        } finally {
            Correlation.setCorrelationId(null);
        }
    }
}
