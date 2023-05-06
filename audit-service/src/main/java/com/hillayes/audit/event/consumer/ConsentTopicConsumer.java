package com.hillayes.audit.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@TopicConsumer(Topic.CONSENT)
@Slf4j
public class ConsentTopicConsumer implements EventConsumer {
    public void consume(EventPacket eventPacket) {
        log.info("Received consent event [payloadClass: {}]", eventPacket.getPayloadClass());
    }
}
