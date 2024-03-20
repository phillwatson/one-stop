package com.hillayes.audit.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.AccountActivity;
import com.hillayes.events.events.auth.AuthenticationFailed;
import com.hillayes.events.events.auth.NewAuthProvider;
import com.hillayes.events.events.auth.UserAuthenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class UserAuthTopicConsumer {
    public void consume(@Observes(during = TransactionPhase.AFTER_SUCCESS)
                        @TopicObserved(Topic.USER_AUTH) EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user_auth event [payloadClass: {}]", payloadClass);

        if (AuthenticationFailed.class.getName().equals(payloadClass)) {
            processAuthenticationFailed(eventPacket.getPayloadContent());
        }

        else if (AccountActivity.class.getName().equals(payloadClass)) {
            processAccountActivity(eventPacket.getPayloadContent());
        }

        else if (UserAuthenticated.class.getName().equals(payloadClass)) {
            processUserAuthenticated(eventPacket.getPayloadContent());
        }

        else if (NewAuthProvider.class.getName().equals(payloadClass)) {
            processNewAuthProvider(eventPacket.getPayloadContent());
        }
    }

    private void processAuthenticationFailed(AuthenticationFailed event) {
        log.info("Login failure [username: {}, authProvider: {}, agent: {}, location: {}]",
            event.getUsername(), event.getAuthProvider(), event.getUserAgent(), event.getUserLocation());
    }

    private void processUserAuthenticated(UserAuthenticated event) {
        log.info("User login [userId: {}, authProvider: {}, agent: {}, location: {}]",
            event.getUserId(), event.getAuthProvider(), event.getUserAgent(), event.getUserLocation());
    }

    private void processNewAuthProvider(NewAuthProvider event) {
        log.info("User login [userId: {}, authProvider: {}, agent: {}, location: {}]",
            event.getUserId(), event.getAuthProvider(), event.getUserAgent(), event.getUserLocation());
    }

    private void processAccountActivity(AccountActivity event) {
        log.info("Account activity [userId: {}, agent: {}, location: {}]",
            event.getUserId(), event.getUserAgent(), event.getUserLocation());
    }
}
