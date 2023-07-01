package com.hillayes.user.event;

import com.hillayes.auth.audit.RequestHeaders;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.AccountActivity;
import com.hillayes.events.events.auth.LoginFailure;
import com.hillayes.events.events.auth.SuspiciousActivity;
import com.hillayes.events.events.auth.UserLogin;
import com.hillayes.events.events.user.*;
import com.hillayes.outbox.sender.EventSender;
import com.hillayes.user.domain.DeletedUser;
import com.hillayes.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserEventSender {
    private final EventSender eventSender;

    public void sendUserRegistered(String email, Duration expires, URI acknowledgerUri) {
        log.debug("Sending UserRegistered event [email: {}]", email);
        List<Locale> languages = RequestHeaders.getAcceptableLanguages();

        eventSender.send(Topic.USER, UserRegistered.builder()
            .email(email)
            .expires(Instant.now().plus(expires))
            .acknowledgerUri(acknowledgerUri)
            .locale(languages.isEmpty() ? null : languages.get(0))
            .build());
    }

    public void sendUserAcknowledged(User user) {
        log.debug("Sending UserAcknowledged event [userId: {}]", user.getId());
        eventSender.send(Topic.USER, UserAcknowledged.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .build());
    }

    public void sendUserCreated(User user) {
        log.debug("Sending UserCreated event [userId: {}]", user.getId());
        eventSender.send(Topic.USER, UserCreated.builder()
            .dateCreated(user.getDateCreated())
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .title(user.getTitle())
            .givenName(user.getGivenName())
            .familyName(user.getFamilyName())
            .preferredName(user.getPreferredName())
            .phoneNumber(user.getPhoneNumber())
            .locale(user.getLocale())
            .build());
    }

    public void sendUserUpdated(User user) {
        log.debug("Sending UserUpdated event [userId: {}]", user.getId());
        eventSender.send(Topic.USER, UserUpdated.builder()
            .dateUpdated(Instant.now())
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .title(user.getTitle())
            .givenName(user.getGivenName())
            .familyName(user.getFamilyName())
            .preferredName(user.getPreferredName())
            .phoneNumber(user.getPhoneNumber())
            .locale(user.getLocale())
            .build());
    }

    public void sendUserDeleted(DeletedUser user) {
        log.debug("Sending UserDeleted event [userId: {}]", user.getId());
        eventSender.send(Topic.USER, UserDeleted.builder()
            .userId(user.getId())
            .dateDeleted(user.getDateDeleted())
            .build());
    }

    public void sendUserLogin(User user) {
        log.debug("Sending UserLogin event [userId: {}]", user.getId());
        eventSender.send(Topic.USER_AUTH, UserLogin.builder()
            .userId(user.getId())
            .dateLogin(Instant.now())
            .userAgent(RequestHeaders.getFirst("User-Agent"))
            .build());
    }

    public void sendLoginFailed(String username, String reason) {
        log.debug("Sending LoginFailed event [username: {}, reason: {}]", username, reason);
        eventSender.send(Topic.USER_AUTH, LoginFailure.builder()
            .username(username)
            .dateLogin(Instant.now())
            .reason(reason)
            .userAgent(RequestHeaders.getFirst("User-Agent"))
            .build());
    }

    public void sendAccountActivity(User user, SuspiciousActivity activity) {
        log.debug("Sending AccountActivity event [userId: {}]", user.getId());
        eventSender.send(Topic.USER, AccountActivity.builder()
            .userId(user.getId())
            .activity(activity)
            .dateRecorded(Instant.now())
            .build());
    }
}
