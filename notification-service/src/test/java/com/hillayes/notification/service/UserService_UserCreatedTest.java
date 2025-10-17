package com.hillayes.notification.service;

import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.repository.UserRepository;
import com.hillayes.notification.task.SendEmailTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class UserService_UserCreatedTest {
    private final UserRepository userRepository = mock();
    private final SendEmailTask sendEmailTask = mock();

    private final UserService fixture = new UserService(
        userRepository,
        sendEmailTask
    );

    @BeforeEach
    public void beforeEach() {
        when(userRepository.save(any())).then(invocation ->
            invocation.getArgument(0)
        );
    }

    @Test
    public void testUserCreated() {
        // given: a UserCreated event
        User user = User.builder()
            .dateCreated(Instant.now())
            .id(UUID.randomUUID())
            .username(randomAlphanumeric(12))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(12))
            .email(randomAlphanumeric(30))
            .locale(Locale.ENGLISH)
            .build();

        // when: the service is called
        fixture.createUser(user);

        // and: an email is sent to the user
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EmailConfiguration.Corresponder> recipientCaptor = ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        verify(sendEmailTask).queueTask(recipientCaptor.capture(), eq(TemplateName.USER_CREATED), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(user.getPreferredName(), recipientCaptor.getValue().getName());
        assertEquals(user.getEmail(), recipientCaptor.getValue().getEmail());
        assertEquals(user.getLocale(), recipientCaptor.getValue().getLocale().get());

        // and: the email template parameters include the user record
        assertSame(user, paramsCaptor.getValue().get("user"));
    }

    @Test
    public void testUserCreated_UserExists_DuplicateEvent() {
        // and: an existing user record
        User existingUser =  User.builder()
            .id(UUID.randomUUID())
            .dateCreated(Instant.now())
            .email(randomAlphanumeric(30))
            .preferredName(randomAlphanumeric(20))
            .locale(Locale.CHINESE)
            .build();
        when(userRepository.findByIdOptional(existingUser.getId())).thenReturn(Optional.of(existingUser));

        // and: a UserCreated event
        User user = User.builder()
            .dateCreated(existingUser.getDateCreated())
            .id(existingUser.getId())
            .username(randomAlphanumeric(12))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(12))
            .email(randomAlphanumeric(30))
            .locale(Locale.ENGLISH)
            .build();

        // when: the service is called
        fixture.createUser(user);

        // and: the user is NOT saved
        verify(userRepository, never()).save(existingUser);

        // and: NO email is sent to the user
        verifyNoInteractions(sendEmailTask);
    }

    @Test
    public void testUserCreated_UserExists_OutOfOrderEvent() {
        // and: an existing user record
        User existingUser =  User.builder()
            .id(UUID.randomUUID())
            .dateCreated(null)
            .dateUpdated(Instant.now())
            .email(randomAlphanumeric(30))
            .preferredName(randomAlphanumeric(20))
            .locale(Locale.CHINESE)
            .build();
        when(userRepository.findByIdOptional(existingUser.getId())).thenReturn(Optional.of(existingUser));

        // and: a UserCreated event
        User user = User.builder()
            .dateCreated(Instant.now().minus(Duration.ofSeconds(10000)))
            .id(existingUser.getId())
            .username(randomAlphanumeric(12))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(12))
            .email(randomAlphanumeric(30))
            .locale(Locale.ENGLISH)
            .build();

        // when: the service is called
        fixture.createUser(user);

        // and: the user is saved
        verify(userRepository).save(existingUser);

        // and: an email is sent to the user
        verify(sendEmailTask).queueTask(any(EmailConfiguration.Corresponder.class), eq(TemplateName.USER_CREATED), any());
    }
}
