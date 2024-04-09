package com.hillayes.rail.event.consumer;

import com.hillayes.events.annotation.TopicObserver;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.rail.service.UserConsentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserTopicConsumer {
    private final UserConsentService userConsentService;

    @TopicObserver
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void consume(@Observes(during = TransactionPhase.AFTER_SUCCESS)
                        @TopicObserved(Topic.USER) EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user event [payloadClass: {}]", payloadClass);

        if (UserDeleted.class.getName().equals(payloadClass)) {
            UserDeleted payload = eventPacket.getPayloadContent();
            userConsentService.deleteAllConsents(payload.getUserId());
        }
    }
}
