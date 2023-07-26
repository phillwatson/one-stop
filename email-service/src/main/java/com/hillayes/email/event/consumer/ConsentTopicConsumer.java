package com.hillayes.email.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@TopicConsumer(Topic.CONSENT)
@RequiredArgsConstructor
@Slf4j
public class ConsentTopicConsumer implements EventConsumer {
    @Transactional
    public void consume(EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received consent event [payloadClass: {}]", payloadClass);

        if (ConsentInitiated.class.getName().equals(payloadClass)) {
            // no action required
        } else if (ConsentGiven.class.getName().equals(payloadClass)) {
            // email to confirm consent was given
        } else if (ConsentDenied.class.getName().equals(payloadClass)) {
            // email to confirm consent was denied
        } else if (ConsentCancelled.class.getName().equals(payloadClass)) {
            // email to confirm that consent was cancelled by user
        } else if (ConsentSuspended.class.getName().equals(payloadClass)) {
            // email to inform that consent has been suspended by rail
            // and include link to grant new consent
        } else if (ConsentExpired.class.getName().equals(payloadClass)) {
            // email to inform that consent has expired
            // and include link to grant new consent
        }
    }
}
