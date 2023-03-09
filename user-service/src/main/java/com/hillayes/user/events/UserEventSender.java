package com.hillayes.user.events;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.LoginFailure;
import com.hillayes.events.events.auth.UserLogin;
import com.hillayes.events.events.user.*;
import com.hillayes.outbox.sender.EventSender;
import com.hillayes.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserEventSender {
    private final EventSender eventSender;

    public void sendUserCreated(User user) {
        log.debug("Sending UserCreated event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER, UserCreated.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .givenName(user.getGivenName())
            .familyName(user.getFamilyName())
            .dateCreated(user.getDateCreated())
            .build());
    }

    public void sendUserDeclined(User user) {
        log.debug("Sending UserDeclined event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER, UserDeclined.builder()
            .userId(user.getId())
            .dateDeclined(Instant.now())
            .build());
    }

    public void sendUserOnboarded(User user) {
        log.debug("Sending UserOnboarded event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER, UserOnboarded.builder()
            .userId(user.getId())
            .dateOnboarded(user.getDateOnboarded())
            .build());
    }

    public void sendUserUpdated(User user) {
        log.debug("Sending UserUpdated event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER, UserUpdated.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .givenName(user.getGivenName())
            .familyName(user.getFamilyName())
            .dateUpdated(Instant.now())
            .build());
    }

    public void sendUserDeleted(User user) {
        log.debug("Sending UserDeleted event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER, UserDeleted.builder()
            .userId(user.getId())
            .dateDeleted(Instant.now())
            .build());
    }

    public void sendUserLogin(User user) {
        log.debug("Sending UserLogin event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER_AUTH, UserLogin.builder()
            .userId(user.getId())
            .dateLogin(Instant.now())
            .build());
    }

    public void sendLoginFailed(String username, String reason) {
        log.debug("Sending LoginFailed event [username: {}, reason: {}]", username, reason);
        eventSender.send(Topic.USER_AUTH, LoginFailure.builder()
            .username(username)
            .dateLogin(Instant.now())
            .reason(reason)
            .build());
    }
}
