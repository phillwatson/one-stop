package com.hillayes.user.events;

import com.hillayes.events.domain.Topic;
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
        UserCreated event = UserCreated.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .dateCreated(user.getDateCreated())
            .build();
        eventSender.send(Topic.USER_CREATED, event);
    }

    public void sendUserDeclined(User user) {
        log.debug("Sending UserDeclined event [username: {}]", user.getUsername());
        UserDeclined event = UserDeclined.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .dateCreated(user.getDateCreated())
            .build();
        eventSender.send(Topic.USER_DECLINED, event);
    }

    public void sendUserOnboarded(User user) {
        log.debug("Sending UserOnboarded event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER_ONBOARDED, UserOnboarded.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .dateCreated(user.getDateCreated())
            .dateOnboarded(user.getDateOnboarded())
            .build());
    }

    public void sendUserUpdated(User user) {
        log.debug("Sending UserUpdated event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER_UPDATED, UserUpdated.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .dateCreated(user.getDateCreated())
            .dateOnboarded(user.getDateOnboarded())
            .build());
    }

    public void sendUserDeleted(User user) {
        log.debug("Sending UserDeleted event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER_DELETED, UserDeleted.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .dateCreated(user.getDateCreated())
            .dateOnboarded(user.getDateOnboarded())
            .build());
    }

    public void sendUserLogin(User user) {
        log.debug("Sending UserLogin event [username: {}]", user.getUsername());
        eventSender.send(Topic.USER_LOGIN, UserLogin.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .dateLogin(Instant.now())
            .build());
    }

    public void sendLoginFailed(String username, String reason) {
        log.debug("Sending LoginFailed event [username: {}]", username);
        eventSender.send(Topic.LOGIN_FAILED, LoginFailure.builder()
            .username(username)
            .dateLogin(Instant.now())
            .reason(reason)
            .build());
    }
}
