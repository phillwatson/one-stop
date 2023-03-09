package com.hillayes.audit.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.executors.correlation.Correlation;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Slf4j
public class UserTopicConsumer {
    @Incoming("user")
    public void consume(EventPacket eventPacket) {
        Correlation.setCorrelationId(eventPacket.getCorrelationId());
        try {
            log.info("Received user event [payloadClass: {}]", eventPacket.getPayloadClass());
        } finally {
            Correlation.setCorrelationId(null);
        }
    }
}
