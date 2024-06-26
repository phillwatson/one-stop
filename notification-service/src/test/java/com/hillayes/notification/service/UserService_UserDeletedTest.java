package com.hillayes.notification.service;

import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.repository.UserRepository;
import com.hillayes.notification.task.SendEmailTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class UserService_UserDeletedTest {
    @Mock
    UserRepository userRepository;

    @Mock
    SendEmailTask sendEmailTask;

    @InjectMocks
    UserService fixture;

    @BeforeEach
    public void setup() {
        openMocks(this);
    }

    @Test
    public void testUserDeleted() {
        // given: an existing user record
        User existingUser =  User.builder()
            .id(UUID.randomUUID())
            .email(randomAlphanumeric(30))
            .preferredName(randomAlphanumeric(20))
            .locale(Locale.CHINESE)
            .build();
        when(userRepository.findByIdOptional(existingUser.getId())).thenReturn(Optional.of(existingUser));

        // when: the service is called
        fixture.deleteUser(existingUser.getId());

        // then: the user record is deleted
        verify(userRepository).delete(existingUser);

        // and: an email is sent to the user
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EmailConfiguration.Corresponder> recipientCaptor = ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        verify(sendEmailTask).queueJob(recipientCaptor.capture(), eq(TemplateName.USER_DELETED), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(existingUser.getPreferredName(), recipientCaptor.getValue().getName());
        assertEquals(existingUser.getEmail(), recipientCaptor.getValue().getEmail());
        assertEquals(existingUser.getLocale(), recipientCaptor.getValue().getLocale().get());

        // and: the email template parameters include the user record
        assertSame(existingUser, paramsCaptor.getValue().get("user"));
    }

    @Test
    public void testUserDeleted_UserNotFound() {
        // given: NO existing user record
        when(userRepository.findByIdOptional(any())).thenReturn(Optional.empty());

        // when: the service is called
        fixture.deleteUser(UUID.randomUUID());

        // then: NO user record is deleted
        verify(userRepository, never()).delete(any());

        // and: NO email is sent to the user
        verifyNoInteractions(sendEmailTask);
    }
}
