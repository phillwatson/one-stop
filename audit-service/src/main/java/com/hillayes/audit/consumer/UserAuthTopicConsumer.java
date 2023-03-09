package com.hillayes.audit.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.events.user.*;
import com.hillayes.executors.correlation.Correlation;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Slf4j
public class UserAuthTopicConsumer {
    @Incoming("user_auth")
    public void consume(EventPacket eventPacket) {
        Correlation.setCorrelationId(eventPacket.getCorrelationId());
        try {
            String payloadClass = eventPacket.getPayloadClass();
            log.info("Received user_auth event [payloadClass: {}]", payloadClass);

            if (LoginFailure.class.getName().equals(payloadClass)) {
                processLoginFailure(eventPacket.getPayloadContent());
            }

            else if (UserCreated.class.getName().equals(payloadClass)) {
                processUserCreated(eventPacket.getPayloadContent());
            }

            else if (UserDeclined.class.getName().equals(payloadClass)) {
                processUserDeclined(eventPacket.getPayloadContent());
            }

            else if (UserDeleted.class.getName().equals(payloadClass)) {
                processUserDeleted(eventPacket.getPayloadContent());
            }

            else if (UserLogin.class.getName().equals(payloadClass)) {
                processUserLogin(eventPacket.getPayloadContent());
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

    private void processLoginFailure(LoginFailure event) {
        log.info("Login failure [username: {}]", event.getUsername());
    }

    private void processUserCreated(UserCreated event) {
        log.info("User created [username: {}]", event.getUsername());
    }

    private void processUserDeclined(UserDeclined event) {
        log.info("User declined [username: {}]", event.getUsername());
    }

    private void processUserDeleted(UserDeleted event) {
        log.info("User deleted [username: {}]", event.getUsername());
    }

    private void processUserLogin(UserLogin event) {
        log.info("User login [username: {}]", event.getUsername());
    }

    private void processUserOnboarded(UserOnboarded event) {
        log.info("User onboarded [username: {}]", event.getUsername());
    }

    private void processUserUpdated(UserUpdated event) {
        log.info("User updated [username: {}]", event.getUsername());
    }
}
