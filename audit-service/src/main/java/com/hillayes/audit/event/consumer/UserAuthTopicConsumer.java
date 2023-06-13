package com.hillayes.audit.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.LoginFailure;
import com.hillayes.events.events.auth.UserLogin;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@TopicConsumer(Topic.USER_AUTH)
@Slf4j
public class UserAuthTopicConsumer implements EventConsumer {
    public void consume(EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user_auth event [payloadClass: {}]", payloadClass);

        if (LoginFailure.class.getName().equals(payloadClass)) {
            processLoginFailure(eventPacket.getPayloadContent());
        }

        else if (UserLogin.class.getName().equals(payloadClass)) {
            processUserLogin(eventPacket.getPayloadContent());
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
