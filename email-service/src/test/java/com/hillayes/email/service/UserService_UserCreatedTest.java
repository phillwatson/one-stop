package com.hillayes.email.service;

import com.hillayes.email.config.EmailConfiguration;
import com.hillayes.email.config.TemplateName;
import com.hillayes.email.domain.User;
import com.hillayes.email.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
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

@QuarkusTest
public class UserService_UserCreatedTest {
    @InjectMock
    UserRepository userRepository;

    @InjectMock
    SendEmailService sendEmailService;

    @Inject
    UserService fixture;

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
        verify(sendEmailService).sendEmail(eq(TemplateName.USER_CREATED), recipientCaptor.capture(), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(user.getPreferredName(), recipientCaptor.getValue().name());
        assertEquals(user.getEmail(), recipientCaptor.getValue().email());
        assertEquals(user.getLocale(), recipientCaptor.getValue().locale().get());

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
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));

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
        verifyNoInteractions(sendEmailService);
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
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));

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
        verify(sendEmailService).sendEmail(eq(TemplateName.USER_CREATED), any(), any());
    }
}
