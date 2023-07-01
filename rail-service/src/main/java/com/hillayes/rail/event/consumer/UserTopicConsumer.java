package com.hillayes.rail.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.rail.service.UserConsentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@TopicConsumer(Topic.USER)
@RequiredArgsConstructor
@Slf4j
public class UserTopicConsumer implements EventConsumer {
    private final UserConsentService userConsentService;

    @Transactional
    public void consume(EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user event [payloadClass: {}]", payloadClass);

        if (UserDeleted.class.getName().equals(payloadClass)) {
            UserDeleted payload = eventPacket.getPayloadContent();
            userConsentService.deleteAllConsents(payload.getUserId());
        }
    }
}
