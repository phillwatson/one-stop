package com.hillayes.audit.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserCreated;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.events.events.user.UserUpdated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes user events to maintain a local DB of user email addresses.
 */
@ApplicationScoped
@Slf4j
public class UserTopicConsumer {
    public void consume(@Observes(during = TransactionPhase.AFTER_SUCCESS)
                        @TopicObserved(Topic.USER) EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user event [payloadClass: {}]", payloadClass);

        if (UserCreated.class.getName().equals(payloadClass)) {
            processUserCreated(eventPacket.getPayloadContent());
        }

        else if (UserDeleted.class.getName().equals(payloadClass)) {
            processUserDeleted(eventPacket.getPayloadContent());
        }

        else if (UserUpdated.class.getName().equals(payloadClass)) {
            processUserUpdated(eventPacket.getPayloadContent());
        }
    }

    private void processUserCreated(UserCreated event) {
        log.info("User created [username: {}]", event.getUsername());
    }

    private void processUserDeleted(UserDeleted event) {
        log.info("User deleted [userId: {}]", event.getUserId());
    }

    private void processUserUpdated(UserUpdated event) {
        log.info("User updated [username: {}]", event.getUsername());
    }
}
