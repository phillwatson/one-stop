package com.hillayes.audit.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.AuthenticationFailed;
import com.hillayes.events.events.auth.UserAuthenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class UserAuthTopicConsumer implements EventConsumer {
    public void consume(@Observes(during = TransactionPhase.AFTER_SUCCESS)
                        @TopicObserved(Topic.USER_AUTH) EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user_auth event [payloadClass: {}]", payloadClass);

        if (AuthenticationFailed.class.getName().equals(payloadClass)) {
            processAuthenticationFailed(eventPacket.getPayloadContent());
        }

        else if (UserAuthenticated.class.getName().equals(payloadClass)) {
            processUserAuthenticated(eventPacket.getPayloadContent());
        }
    }

    private void processAuthenticationFailed(AuthenticationFailed event) {
        log.info("Login failure [username: {}]", event.getUsername());
        throw new RuntimeException("Test event retry");
    }

    private void processUserAuthenticated(UserAuthenticated event) {
        log.info("User login [userId: {}]", event.getUserId());
    }
}
