package com.hillayes.email.service;

import com.hillayes.email.config.EmailConfiguration;
import com.hillayes.email.config.TemplateName;
import com.hillayes.email.domain.User;
import com.hillayes.email.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
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
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));

        // when: the service is called
        fixture.deleteUser(existingUser.getId());

        // then: the user record is deleted
        verify(userRepository).delete(existingUser);

        // and: an email is sent to the user
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EmailConfiguration.Corresponder> recipientCaptor = ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        verify(sendEmailService).sendEmail(eq(TemplateName.USER_DELETED), recipientCaptor.capture(), paramsCaptor.capture());

        // and: the recipient details are taken from the event payload
        assertEquals(existingUser.getPreferredName(), recipientCaptor.getValue().name());
        assertEquals(existingUser.getEmail(), recipientCaptor.getValue().email());
        assertEquals(existingUser.getLocale(), recipientCaptor.getValue().locale().get());

        // and: the email template parameters include the user record
        assertSame(existingUser, paramsCaptor.getValue().get("user"));
    }

    @Test
    public void testUserDeleted_UserNotFound() {
        // given: NO existing user record
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        // when: the service is called
        fixture.deleteUser(UUID.randomUUID());

        // then: NO user record is deleted
        verify(userRepository, never()).delete(any());

        // and: NO email is sent to the user
        verifyNoInteractions(sendEmailService);
    }
}
