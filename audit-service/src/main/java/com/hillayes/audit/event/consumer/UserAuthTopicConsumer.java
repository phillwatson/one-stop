package com.hillayes.audit.event.consumer;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.events.auth.LoginFailure;
import com.hillayes.events.events.auth.UserLogin;
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

            else if (UserLogin.class.getName().equals(payloadClass)) {
                processUserLogin(eventPacket.getPayloadContent());
            }
        } finally {
            Correlation.setCorrelationId(null);
        }
    }

    private void processLoginFailure(LoginFailure event) {
        log.info("Login failure [username: {}]", event.getUsername());
        throw new RuntimeException("Test event retry");
    }

    private void processUserLogin(UserLogin event) {
        log.info("User login [userId: {}]", event.getUserId());
    }
}
