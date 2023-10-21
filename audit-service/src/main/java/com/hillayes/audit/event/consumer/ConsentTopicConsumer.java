package com.hillayes.audit.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ConsentTopicConsumer {
    public void consume(@Observes(during = TransactionPhase.AFTER_SUCCESS)
                        @TopicObserved(Topic.CONSENT) EventPacket eventPacket) {
        log.info("Received consent event [payloadClass: {}]", eventPacket.getPayloadClass());
    }
}
