package com.hillayes.user.event;

import com.hillayes.auth.audit.RequestHeaders;
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
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class UserEventSenderTest {
    @InjectMock
    EventSender eventSender;
    @InjectMock
    RequestHeaders requestHeaders;

    @Inject
    UserEventSender fixture;

    @Test
    public void testSendUserRegistered() {
        // given: a registered user's email and acknowledgement details
        String email = randomAlphanumeric(30);
        Duration expires = Duration.ofMinutes(15);
        URI acknowledgerUri = URI.create("http://onestop/users/onboard");

        // and: a locale in the original client request headers
        when(requestHeaders.getAcceptableLanguages()).thenReturn(List.of(Locale.CHINESE));

        // when: the fixture is called
        Instant expiresAt = Instant.now().plus(expires);
        fixture.sendUserRegistered(email, expires, acknowledgerUri);

        // then: the correct event is emitted
        ArgumentCaptor<UserRegistered> captor = ArgumentCaptor.forClass(UserRegistered.class);
        verify(eventSender).send(eq(Topic.USER), captor.capture());

        // and: the content is correct
        UserRegistered event = captor.getValue();
        assertEquals(email, event.getEmail());
        assertEquals(expiresAt.getEpochSecond() / 10, event.getExpires().getEpochSecond() / 10);
        assertEquals(acknowledgerUri, event.getAcknowledgerUri());
        assertEquals(Locale.CHINESE, event.getLocale());
    }

    @Test
    public void testSendUserCreated() {
        // given: a user that has been created
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(20))
            .passwordHash(randomAlphanumeric(10))
            .email(randomAlphanumeric(30))
            .title(randomAlphanumeric(10))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(25))
            .preferredName(randomAlphanumeric(15))
            .phoneNumber(randomNumeric(10))
            .locale(Locale.GERMAN)
            .dateOnboarded(Instant.now())
            .dateCreated(Instant.now())
            .roles(Set.of("user"))
            .build();

        // and: a locale in the original client request headers
        when(requestHeaders.getAcceptableLanguages()).thenReturn(List.of(user.getLocale()));

        // when: the fixure is called
        fixture.sendUserCreated(user);

        // then: the correct event is emitted
        ArgumentCaptor<UserCreated> captor = ArgumentCaptor.forClass(UserCreated.class);
        verify(eventSender).send(eq(Topic.USER), captor.capture());

        // and: the content is correct
        UserCreated event = captor.getValue();
        assertEquals(user.getId(), event.getUserId());
        assertEquals(user.getUsername(), event.getUsername());
        assertEquals(user.getPhoneNumber(), event.getPhoneNumber());
        assertEquals(user.getEmail(), event.getEmail());
        assertEquals(user.getDateCreated(), event.getDateCreated());
        assertEquals(user.getLocale(), event.getLocale());
        assertEquals(user.getTitle(), event.getTitle());
        assertEquals(user.getFamilyName(), event.getFamilyName());
        assertEquals(user.getGivenName(), event.getGivenName());
        assertEquals(user.getPreferredName(), event.getPreferredName());
    }

    @Test
    public void testSendUserUpdated() {
        // given: a user that has been updated
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(20))
            .passwordHash(randomAlphanumeric(10))
            .email(randomAlphanumeric(30))
            .title(randomAlphanumeric(10))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(25))
            .preferredName(randomAlphanumeric(15))
            .phoneNumber(randomNumeric(10))
            .locale(Locale.GERMAN)
            .dateOnboarded(Instant.now())
            .dateCreated(Instant.now())
            .roles(Set.of("user"))
            .build();

        // and: a locale in the original client request headers
        when(requestHeaders.getAcceptableLanguages()).thenReturn(List.of(user.getLocale()));

        // when: the fixure is called
        fixture.sendUserUpdated(user);

        // then: the correct event is emitted
        ArgumentCaptor<UserUpdated> captor = ArgumentCaptor.forClass(UserUpdated.class);
        verify(eventSender).send(eq(Topic.USER), captor.capture());

        // and: the content is correct
        UserUpdated event = captor.getValue();
        assertEquals(user.getId(), event.getUserId());
        assertEquals(user.getUsername(), event.getUsername());
        assertEquals(user.getPhoneNumber(), event.getPhoneNumber());
        assertEquals(user.getEmail(), event.getEmail());
        assertEquals(user.getLocale(), event.getLocale());
        assertEquals(user.getTitle(), event.getTitle());
        assertEquals(user.getFamilyName(), event.getFamilyName());
        assertEquals(user.getGivenName(), event.getGivenName());
        assertEquals(user.getPreferredName(), event.getPreferredName());
        assertNotNull(event.getDateUpdated());
    }

    @Test
    public void testSendUserDeleted() {
        // given: a user that has been deleted
        DeletedUser user = DeletedUser.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(20))
            .passwordHash(randomAlphanumeric(10))
            .email(randomAlphanumeric(30))
            .title(randomAlphanumeric(10))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(25))
            .preferredName(randomAlphanumeric(15))
            .phoneNumber(randomNumeric(10))
            .locale(Locale.GERMAN)
            .dateOnboarded(Instant.now())
            .dateCreated(Instant.now())
            .dateDeleted(Instant.now())
            .build();

        // and: a locale in the original client request headers
        when(requestHeaders.getAcceptableLanguages()).thenReturn(List.of(user.getLocale()));

        // when: the fixure is called
        fixture.sendUserDeleted(user);

        // then: the correct event is emitted
        ArgumentCaptor<UserDeleted> captor = ArgumentCaptor.forClass(UserDeleted.class);
        verify(eventSender).send(eq(Topic.USER), captor.capture());

        // and: the content is correct
        UserDeleted event = captor.getValue();
        assertEquals(user.getId(), event.getUserId());
        assertEquals(user.getDateDeleted(), event.getDateDeleted());
    }

    @Test
    public void testSendUserAuthenticated() {
        // given: a user has logged in
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(20))
            .passwordHash(randomAlphanumeric(10))
            .email(randomAlphanumeric(30))
            .title(randomAlphanumeric(10))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(25))
            .preferredName(randomAlphanumeric(15))
            .phoneNumber(randomNumeric(10))
            .locale(Locale.GERMAN)
            .dateOnboarded(Instant.now())
            .dateCreated(Instant.now())
            .roles(Set.of("user"))
            .build();

        // and: a locale in the original client request headers
        when(requestHeaders.getAcceptableLanguages()).thenReturn(List.of(user.getLocale()));

        // and: a user-agent is identified by the request headers
        when(requestHeaders.getFirst("User-Agent")).thenReturn("ssssss");

        // when: the fixure is called
        fixture.sendUserAuthenticated(user);

        // then: the correct event is emitted
        ArgumentCaptor<UserAuthenticated> captor = ArgumentCaptor.forClass(UserAuthenticated.class);
        verify(eventSender).send(eq(Topic.USER_AUTH), captor.capture());

        // and: the content is correct
        UserAuthenticated event = captor.getValue();
        assertEquals(user.getId(), event.getUserId());
        assertNotNull(event.getDateLogin());
        assertEquals(requestHeaders.getFirst("User-Agent"), event.getUserAgent());
    }

    @Test
    public void testSendAuthenticationFailed() {
        // given: a user has failed to authenticate - and a reason
        String username = randomAlphanumeric(20);
        String reason = randomAlphanumeric(10);

        // and: a user-agent is identified by the request headers
        when(requestHeaders.getFirst("User-Agent")).thenReturn("ssssss");

        // when: the fixure is called
        fixture.sendAuthenticationFailed(username, reason);

        // then: the correct event is emitted
        ArgumentCaptor<AuthenticationFailed> captor = ArgumentCaptor.forClass(AuthenticationFailed.class);
        verify(eventSender).send(eq(Topic.USER_AUTH), captor.capture());

        // and: the content is correct
        AuthenticationFailed event = captor.getValue();
        assertEquals(username, event.getUsername());
        assertEquals(reason, event.getReason());
        assertNotNull(event.getDateLogin());
        assertEquals(requestHeaders.getFirst("User-Agent"), event.getUserAgent());
    }

    @Test
    public void testSendAccountActivity() {
        // given: a user that has been created
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(20))
            .passwordHash(randomAlphanumeric(10))
            .email(randomAlphanumeric(30))
            .title(randomAlphanumeric(10))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(25))
            .preferredName(randomAlphanumeric(15))
            .phoneNumber(randomNumeric(10))
            .locale(Locale.GERMAN)
            .dateOnboarded(Instant.now())
            .dateCreated(Instant.now())
            .roles(Set.of("user"))
            .build();

        // and: suspicious activity has been see
        SuspiciousActivity activity = SuspiciousActivity.EMAIL_REGISTRATION;

        // when: the fixure is called
        fixture.sendAccountActivity(user, activity);

        // then: the correct event is emitted
        ArgumentCaptor<AccountActivity> captor = ArgumentCaptor.forClass(AccountActivity.class);
        verify(eventSender).send(eq(Topic.USER), captor.capture());

        // and: the content is correct
        AccountActivity event = captor.getValue();
        assertEquals(user.getId(), event.getUserId());
        assertEquals(activity, event.getActivity());
        assertNotNull(event.getDateRecorded());
    }
}
