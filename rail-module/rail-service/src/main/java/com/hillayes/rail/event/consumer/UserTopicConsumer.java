package com.hillayes.rail.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.annotation.TopicObserver;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.rail.service.CategoryService;
import com.hillayes.rail.service.UserConsentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserTopicConsumer {
    private final UserConsentService userConsentService;
    private final CategoryService categoryService;

    // May require @Observes(during = TransactionPhase.AFTER_SUCCESS) if the
    // event is triggered in the same transaction as it is consumed.
    // As we're using the outbox pattern, we can assume that the trigger that
    // caused the event has been committed.

    @TopicObserver
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void consume(@Observes
                        @TopicObserved(Topic.USER) EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user event [payloadClass: {}]", payloadClass);

        if (UserDeleted.class.getName().equals(payloadClass)) {
            UserDeleted payload = eventPacket.getPayloadContent();
            userConsentService.deleteAllConsents(payload.getUserId());
            categoryService.deleteAllCategoryGroups(payload.getUserId());
        }
    }
}
