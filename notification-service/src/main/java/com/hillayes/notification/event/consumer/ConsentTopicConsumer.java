package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@TopicConsumer(Topic.CONSENT)
@RequiredArgsConstructor
@Slf4j
public class ConsentTopicConsumer implements EventConsumer {
    @Transactional
    public void consume(EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received consent event [payloadClass: {}]", payloadClass);

        Map<String, Object> params = new HashMap<>();
        if (ConsentInitiated.class.getName().equals(payloadClass)) {
            // no action required
        }

        else if (ConsentGiven.class.getName().equals(payloadClass)) {
            ConsentGiven event = eventPacket.getPayloadContent();
        }

        else if (ConsentDenied.class.getName().equals(payloadClass)) {
            ConsentDenied event = eventPacket.getPayloadContent();
        }

        else if (ConsentCancelled.class.getName().equals(payloadClass)) {
            ConsentCancelled event = eventPacket.getPayloadContent();
        }

        else if (ConsentSuspended.class.getName().equals(payloadClass)) {
            ConsentSuspended event = eventPacket.getPayloadContent();
        }

        else if (ConsentExpired.class.getName().equals(payloadClass)) {
            ConsentExpired event = eventPacket.getPayloadContent();
        }
    }
}
