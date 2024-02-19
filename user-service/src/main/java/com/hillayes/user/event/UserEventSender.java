package com.hillayes.user.event;

import com.hillayes.auth.audit.RequestHeaders;
import com.hillayes.commons.Strings;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.AccountActivity;
import com.hillayes.events.events.auth.AuthenticationFailed;
import com.hillayes.events.events.auth.SuspiciousActivity;
import com.hillayes.events.events.auth.UserAuthenticated;
import com.hillayes.events.events.user.UserCreated;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.events.events.user.UserRegistered;
import com.hillayes.events.events.user.UserUpdated;
import com.hillayes.outbox.sender.EventSender;
import com.hillayes.user.domain.DeletedUser;
import com.hillayes.user.domain.User;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final RequestHeaders requestHeaders;

    public void sendUserRegistered(String email, Duration expires, URI acknowledgerUri) {
        log.debug("Sending UserRegistered event [email: {}]", Strings.maskEmail(email));
        List<Locale> languages = requestHeaders.getAcceptableLanguages();

        eventSender.send(Topic.USER, UserRegistered.builder()
            .email(email)
            .expires(Instant.now().plus(expires))
            .acknowledgerUri(acknowledgerUri)
            .locale(languages.isEmpty() ? null : languages.get(0))
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

    public void sendUserAuthenticated(User user) {
        log.debug("Sending UserAuthenticated event [userId: {}]", user.getId());
        eventSender.send(Topic.USER_AUTH, UserAuthenticated.builder()
            .userId(user.getId())
            .dateLogin(Instant.now())
            .userAgent(requestHeaders.getFirst("User-Agent"))
            .build());
    }

    public void sendAuthenticationFailed(String username, String reason) {
        log.debug("Sending AuthenticationFailed event [username: {}, reason: {}]", username, reason);
        eventSender.send(Topic.USER_AUTH, AuthenticationFailed.builder()
            .username(username)
            .dateLogin(Instant.now())
            .reason(reason)
            .userAgent(requestHeaders.getFirst("User-Agent"))
            .build());
    }

    public void sendAccountActivity(User user, SuspiciousActivity activity) {
        log.debug("Sending AccountActivity event [userId: {}]", user.getId());
        eventSender.send(Topic.USER, AccountActivity.builder()
            .userId(user.getId())
            .activity(activity)
            .dateRecorded(Instant.now())
            .userAgent(requestHeaders.getFirst("User-Agent"))
            .build());
    }
}
