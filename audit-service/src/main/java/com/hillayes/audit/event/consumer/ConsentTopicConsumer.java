package com.hillayes.audit.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.annotation.TopicObserver;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ConsentTopicConsumer {
    @TopicObserver
    public void consume(@Observes
                        @TopicObserved(Topic.CONSENT) EventPacket eventPacket) {
        log.info("Received consent event [payloadClass: {}]", eventPacket.getPayloadClass());
    }
}
