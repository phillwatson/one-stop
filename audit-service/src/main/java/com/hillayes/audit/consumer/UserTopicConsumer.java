package com.hillayes.audit.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.events.user.*;
import com.hillayes.executors.correlation.Correlation;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;

/**
 * Consumes user events to maintain a local DB of user email addresses.
 */
@ApplicationScoped
@Slf4j
public class UserTopicConsumer {
    @Incoming("user")
    public void consume(EventPacket eventPacket) {
        Correlation.setCorrelationId(eventPacket.getCorrelationId());
        try {
            String payloadClass = eventPacket.getPayloadClass();
            log.info("Received user_auth event [payloadClass: {}]", payloadClass);

            if (UserCreated.class.getName().equals(payloadClass)) {
                processUserCreated(eventPacket.getPayloadContent());
            }

            else if (UserDeclined.class.getName().equals(payloadClass)) {
                processUserDeclined(eventPacket.getPayloadContent());
            }

            else if (UserDeleted.class.getName().equals(payloadClass)) {
                processUserDeleted(eventPacket.getPayloadContent());
            }

            else if (UserOnboarded.class.getName().equals(payloadClass)) {
                processUserOnboarded(eventPacket.getPayloadContent());
            }

            else if (UserUpdated.class.getName().equals(payloadClass)) {
                processUserUpdated(eventPacket.getPayloadContent());
            }
        } finally {
            Correlation.setCorrelationId(null);
        }
    }

    private void processUserCreated(UserCreated event) {
        log.info("User created [username: {}]", event.getUsername());
    }

    private void processUserDeclined(UserDeclined event) {
        log.info("User declined [userId: {}]", event.getUserId());
    }

    private void processUserDeleted(UserDeleted event) {
        log.info("User deleted [userId: {}]", event.getUserId());
    }

    private void processUserOnboarded(UserOnboarded event) {
        log.info("User onboarded [userId: {}]", event.getUserId());
    }

    private void processUserUpdated(UserUpdated event) {
        log.info("User updated [username: {}]", event.getUsername());
    }
}
