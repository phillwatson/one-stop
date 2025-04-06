package com.hillayes.audit.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.annotation.TopicObserver;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserCreated;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.events.events.user.UserUpdated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes user events to maintain a local DB of user email addresses.
 */
@ApplicationScoped
@Slf4j
public class UserTopicConsumer {
    @TopicObserver
    public void consume(@Observes
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
        log.info("User created [userId: {}]", event.getUserId());
    }

    private void processUserDeleted(UserDeleted event) {
        log.info("User deleted [userId: {}]", event.getUserId());
    }

    private void processUserUpdated(UserUpdated event) {
        log.info("User updated [userId: {}]", event.getUserId());
    }
}
