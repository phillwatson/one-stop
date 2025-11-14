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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class UserService_UserUpdatedTest {
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
        when(userRepository.findByIdOptional(existingUser.getId())).thenReturn(Optional.of(existingUser));
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
        verify(sendEmailTask, times(2)).queueTask(recipientCaptor.capture(), eq(TemplateName.USER_UPDATED), paramsCaptor.capture());

        // and: one to the recipient details are taken from the event payload
        recipientCaptor.getAllValues().stream()
            .filter(recipient -> recipient.getEmail().equals(user.getEmail()))
            .findFirst()
            .ifPresentOrElse(
                recipient -> {
                    assertEquals(user.getPreferredName(), recipient.getName());
                    assertEquals(user.getEmail(), recipient.getEmail());
                    assertEquals(user.getLocale(), recipient.getLocale().get());
                },
                () -> fail("Missing recipient for updated user")
            );

        // and: the other is sent to the old email address
        recipientCaptor.getAllValues().stream()
            .filter(recipient -> recipient.getEmail().equals(originalRecipient.getEmail()))
            .findFirst()
            .ifPresentOrElse(
                recipient -> {
                    assertEquals(originalRecipient.getName(), recipient.getName());
                    assertEquals(originalRecipient.getEmail(), recipient.getEmail());
                    assertEquals(originalRecipient.getLocale().get(), recipient.getLocale().get());
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
        when(userRepository.findByIdOptional(existingUser.getId())).thenReturn(Optional.of(existingUser));
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
        verify(sendEmailTask, times(2)).queueTask(recipientCaptor.capture(), eq(TemplateName.USER_UPDATED), paramsCaptor.capture());

        // and: one to the recipient details are taken from the event payload
        recipientCaptor.getAllValues().stream()
            .filter(recipient -> recipient.getEmail().equals(user.getEmail()))
            .findFirst()
            .ifPresentOrElse(
                recipient -> {
                    assertEquals(user.getPreferredName(), recipient.getName());
                    assertEquals(user.getEmail(), recipient.getEmail());
                    assertEquals(user.getLocale(), recipient.getLocale().get());
                },
                () -> fail("Missing recipient for updated user")
            );

        // and: the other is sent to the old email address
        recipientCaptor.getAllValues().stream()
            .filter(recipient -> recipient.getEmail().equals(originalRecipient.getEmail()))
            .findFirst()
            .ifPresentOrElse(
                recipient -> {
                    assertEquals(originalRecipient.getName(), recipient.getName());
                    assertEquals(originalRecipient.getEmail(), recipient.getEmail());
                    assertEquals(originalRecipient.getLocale().get(), recipient.getLocale().get());
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
        when(userRepository.findByIdOptional(existingUser.getId())).thenReturn(Optional.of(existingUser));

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
        verify(sendEmailTask, times(1)).queueTask(recipientCaptor.capture(), eq(TemplateName.USER_UPDATED), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(user.getPreferredName(), recipientCaptor.getValue().getName());
        assertEquals(user.getEmail(), recipientCaptor.getValue().getEmail());
        assertEquals(user.getLocale(), recipientCaptor.getValue().getLocale().get());

        // and: the email template parameters include the updated user record
        assertEquals(existingUser, paramsCaptor.getValue().get("user"));
    }

    @Test
    public void testUserUpdated_UserNotFound() {
        // given: the user does not exist
        when(userRepository.findByIdOptional(any())).thenReturn(Optional.empty());

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
        verify(sendEmailTask, times(1)).queueTask(recipientCaptor.capture(), eq(TemplateName.USER_UPDATED), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(user.getPreferredName(), recipientCaptor.getValue().getName());
        assertEquals(user.getEmail(), recipientCaptor.getValue().getEmail());
        assertEquals(user.getLocale(), recipientCaptor.getValue().getLocale().get());

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
        when(userRepository.findByIdOptional(existingUser.getId())).thenReturn(Optional.of(existingUser));

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
        verifyNoInteractions(sendEmailTask);
    }
}
