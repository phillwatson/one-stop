package com.hillayes.notification.service;

import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

@QuarkusTest
public class UserService_UserDeletedTest {
    @InjectMock
    UserRepository userRepository;

    @InjectMock
    SendEmailService sendEmailService;

    @Inject
    UserService fixture;

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
        verify(sendEmailService).sendEmail(eq(TemplateName.USER_DELETED), recipientCaptor.capture(), paramsCaptor.capture());

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
        verifyNoInteractions(sendEmailService);
    }
}
