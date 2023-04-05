package com.hillayes.email.event.consumer;

import com.hillayes.email.domain.User;
import com.hillayes.email.service.UserService;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.events.user.*;
import com.hillayes.executors.correlation.Correlation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserTopicConsumer {
    private final UserService userService;
    
    @Incoming("user")
    @Transactional
    public void consume(EventPacket eventPacket) {
        Correlation.setCorrelationId(eventPacket.getCorrelationId());
        try {
            String payloadClass = eventPacket.getPayloadClass();
            log.info("Received user event [payloadClass: {}]", payloadClass);

            if (UserCreated.class.getName().equals(payloadClass)) {
                processUserCreated(eventPacket.getPayloadContent());
            }

            else if (UserOnboarded.class.getName().equals(payloadClass)) {
                processUserOnboarded(eventPacket.getPayloadContent());
            }

            else if (UserDeclined.class.getName().equals(payloadClass)) {
                processUserDeclined(eventPacket.getPayloadContent());
            }

            else if (UserDeleted.class.getName().equals(payloadClass)) {
                processUserDeleted(eventPacket.getPayloadContent());
            }

            else if (UserUpdated.class.getName().equals(payloadClass)) {
                processUserUpdated(eventPacket.getPayloadContent());
            }
        } finally {
            Correlation.setCorrelationId(null);
        }
    }

    private void processUserCreated(UserCreated event) {
        User user = User.builder()
            .id(event.getUserId())
            .username(event.getUsername())
            .email(event.getEmail())
            .title(event.getTitle())
            .givenName(event.getGivenName())
            .familyName(event.getFamilyName())
            .preferredName(event.getPreferredName())
            .build();
        userService.createUser(user);
    }

    private void processUserOnboarded(UserOnboarded event) {
        userService.onboardUser(event.getUserId());
    }

    private void processUserDeclined(UserDeclined event) {
        userService.deleteUser(event.getUserId());
    }

    private void processUserDeleted(UserDeleted event) {
        userService.deleteUser(event.getUserId());
    }

    private void processUserUpdated(UserUpdated event) {
        User user = User.builder()
            .id(event.getUserId())
            .username(event.getUsername())
            .email(event.getEmail())
            .title(event.getTitle())
            .givenName(event.getGivenName())
            .familyName(event.getFamilyName())
            .preferredName(event.getPreferredName())
            .build();
        userService.updateUser(user);
    }
}
