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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class UserService_UserUpdatedTest {
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
    public void testUserUpdated_NewEmailAddress() {
        // and: an existing user record - never updated before
        User existingUser =  User.builder()
            .id(UUID.randomUUID())
            .dateCreated(Instant.now().minus(Duration.ofSeconds(20000)))
            .dateUpdated(null)
            .email(randomAlphanumeric(30))
            .preferredName(randomAlphanumeric(20))
            .locale(Locale.CHINESE)
            .build();
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        // take snapshot as this user will be updated
        SendEmailService.Recipient originalRecipient = new SendEmailService.Recipient(existingUser);

        // and: a UserUpdated event to update that user
        User user = User.builder()
            .dateUpdated(Instant.now())
            .id(existingUser.getId())
            .email(randomAlphanumeric(30)) // different from existing user
            .username(randomAlphanumeric(12))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(12))
            .locale(Locale.ENGLISH)
            .build();

        // when: the service is called
        fixture.updateUser(user);

        // then: the user details are saved
        verify(userRepository).save(existingUser);

        // and: two emails are sent
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EmailConfiguration.Corresponder> recipientCaptor = ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        verify(sendEmailService, times(2)).sendEmail(eq(TemplateName.USER_UPDATED), recipientCaptor.capture(), paramsCaptor.capture());

        // and: one to the recipient details are taken from the event payload
        recipientCaptor.getAllValues().stream()
            .filter(recipient -> recipient.email().equals(user.getEmail()))
            .findFirst()
            .ifPresentOrElse(
                recipient -> {
                    assertEquals(user.getPreferredName(), recipient.name());
                    assertEquals(user.getEmail(), recipient.email());
                    assertEquals(user.getLocale(), recipient.locale().get());
                },
                () -> fail("Missing recipient for updated user")
            );

        // and: the other is sent to the old email address
        recipientCaptor.getAllValues().stream()
            .filter(recipient -> recipient.email().equals(originalRecipient.email()))
            .findFirst()
            .ifPresentOrElse(
                recipient -> {
                    assertEquals(originalRecipient.name(), recipient.name());
                    assertEquals(originalRecipient.email(), recipient.email());
                    assertEquals(originalRecipient.locale().get(), recipient.locale().get());
                },
                () -> fail("Missing recipient for original user")
            );

        // and: the email template parameters include the updated user record
        paramsCaptor.getAllValues().forEach( params ->
            assertEquals(existingUser, params.get("user"))
        );
    }

    @Test
    public void testUserUpdated_PreviousUpdated() {
        // and: an existing user record - updated before
        User existingUser =  User.builder()
            .id(UUID.randomUUID())
            .dateCreated(Instant.now().minus(Duration.ofSeconds(20000)))
            .dateUpdated(Instant.now().minus(Duration.ofSeconds(10000)))
            .email(randomAlphanumeric(30))
            .preferredName(randomAlphanumeric(20))
            .locale(Locale.CHINESE)
            .build();
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        // take snapshot as this user will be updated
        SendEmailService.Recipient originalRecipient = new SendEmailService.Recipient(existingUser);

        // and: a UserUpdated event to update that user
        User user = User.builder()
            .dateUpdated(Instant.now())
            .id(existingUser.getId())
            .email(randomAlphanumeric(30)) // different from existing user
            .username(randomAlphanumeric(12))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(12))
            .locale(Locale.ENGLISH)
            .build();

        // when: the service is called
        fixture.updateUser(user);

        // then: the user details are saved
        verify(userRepository).save(existingUser);

        // and: two emails are sent
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EmailConfiguration.Corresponder> recipientCaptor = ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        verify(sendEmailService, times(2)).sendEmail(eq(TemplateName.USER_UPDATED), recipientCaptor.capture(), paramsCaptor.capture());

        // and: one to the recipient details are taken from the event payload
        recipientCaptor.getAllValues().stream()
            .filter(recipient -> recipient.email().equals(user.getEmail()))
            .findFirst()
            .ifPresentOrElse(
                recipient -> {
                    assertEquals(user.getPreferredName(), recipient.name());
                    assertEquals(user.getEmail(), recipient.email());
                    assertEquals(user.getLocale(), recipient.locale().get());
                },
                () -> fail("Missing recipient for updated user")
            );

        // and: the other is sent to the old email address
        recipientCaptor.getAllValues().stream()
            .filter(recipient -> recipient.email().equals(originalRecipient.email()))
            .findFirst()
            .ifPresentOrElse(
                recipient -> {
                    assertEquals(originalRecipient.name(), recipient.name());
                    assertEquals(originalRecipient.email(), recipient.email());
                    assertEquals(originalRecipient.locale().get(), recipient.locale().get());
                },
                () -> fail("Missing recipient for original user")
            );

        // and: the email template parameters include the updated user record
        paramsCaptor.getAllValues().forEach( params ->
            assertEquals(existingUser, params.get("user"))
        );
    }

    @Test
    public void testUserUpdated_SameEmailAddress() {
        // and: an existing user record
        User existingUser =  User.builder()
            .dateUpdated(Instant.now().minus(Duration.ofSeconds(10000)))
            .id(UUID.randomUUID())
            .email(randomAlphanumeric(30))
            .preferredName(randomAlphanumeric(20))
            .locale(Locale.CHINESE)
            .build();
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));

        // and: a UserUpdated event to update that user
        User user = User.builder()
            .dateUpdated(Instant.now())
            .id(existingUser.getId())
            .username(randomAlphanumeric(12))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(12))
            .email(existingUser.getEmail()) // the same as original
            .locale(Locale.ENGLISH)
            .build();

        // when: the service is called
        fixture.updateUser(user);

        // and: the user details are saved
        verify(userRepository).save(existingUser);

        // and: an email is sent to the new email address
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EmailConfiguration.Corresponder> recipientCaptor = ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        verify(sendEmailService, times(1)).sendEmail(eq(TemplateName.USER_UPDATED), recipientCaptor.capture(), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(user.getPreferredName(), recipientCaptor.getValue().name());
        assertEquals(user.getEmail(), recipientCaptor.getValue().email());
        assertEquals(user.getLocale(), recipientCaptor.getValue().locale().get());

        // and: the email template parameters include the updated user record
        assertEquals(existingUser, paramsCaptor.getValue().get("user"));
    }

    @Test
    public void testUserUpdated_UserNotFound() {
        // given: the user does not exist
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        // and: a UserUpdated event to update that user
        User user = User.builder()
            .dateUpdated(Instant.now())
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
        fixture.updateUser(user);

        // then: the user details are saved
        verify(userRepository).save(user);

        // and: an email is sent to the new email address
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EmailConfiguration.Corresponder> recipientCaptor = ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        verify(sendEmailService, times(1)).sendEmail(eq(TemplateName.USER_UPDATED), recipientCaptor.capture(), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(user.getPreferredName(), recipientCaptor.getValue().name());
        assertEquals(user.getEmail(), recipientCaptor.getValue().email());
        assertEquals(user.getLocale(), recipientCaptor.getValue().locale().get());

        // and: the email template parameters include the updated user record
        assertEquals(user, paramsCaptor.getValue().get("user"));
    }

    @Test
    public void testUserUpdated_DuplicateOrOutOfOrder() {
        // and: an existing user record
        User existingUser =  User.builder()
            .id(UUID.randomUUID())
            .dateUpdated(Instant.now().minus(Duration.ofSeconds(1000)))
            .email(randomAlphanumeric(30))
            .preferredName(randomAlphanumeric(20))
            .locale(Locale.CHINESE)
            .build();
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));

        // and: a UserUpdated event to update that user - with older update date
        User user = User.builder()
            .dateUpdated(Instant.now().minus(Duration.ofSeconds(2000)))
            .id(existingUser.getId())
            .email(randomAlphanumeric(30))
            .username(randomAlphanumeric(12))
            .title(randomAlphanumeric(5))
            .givenName(randomAlphanumeric(20))
            .familyName(randomAlphanumeric(20))
            .preferredName(randomAlphanumeric(20))
            .phoneNumber(randomNumeric(12))
            .locale(Locale.ENGLISH)
            .build();

        // when: the service is called
        fixture.updateUser(user);

        // then: NOT user details are updated
        verify(userRepository, never()).save(any());

        // and: NO emails are sent
        verifyNoInteractions(sendEmailService);
    }
}
