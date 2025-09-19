package com.hillayes.notification.task;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.service.SendEmailService;
import com.hillayes.notification.service.UserService;
import com.hillayes.notification.config.TemplateName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendEmailTaskTest {
    @Mock
    UserService userService;

    @Mock
    SendEmailService sendEmailService;

    @Mock
    SchedulerFactory scheduler;

    @InjectMocks
    SendEmailTask fixture;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        fixture = new SendEmailTask(userService, sendEmailService);
        fixture.taskInitialised(scheduler);
    }

    @Test
    public void testQueueTask_UserId() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a collection of template parameters
        Map<String, Object> params = Map.of(
            "param1", "value1",
            "param2", "value2"
        );

        // when: queueing a task
        fixture.queueTask(userId, TemplateName.USER_REGISTERED, params);

        // then: the task is queued
        ArgumentCaptor<SendEmailTask.Payload> paramCaptor = ArgumentCaptor.forClass(SendEmailTask.Payload.class);
        verify(scheduler).addJob(eq(fixture), paramCaptor.capture());
        SendEmailTask.Payload payload = paramCaptor.getValue();

        // and: the user id is passed
        assertEquals(userId, payload.userId);

        // and: the template name is passed
        assertEquals(TemplateName.USER_REGISTERED, payload.templateName);

        // and: the template parameters are passed
        assertEquals(params, payload.params);

        // and: NO recipient is passed
        assertNull(payload.recipient);
    }

    @Test
    public void testQueueTask_Recipient() {
        // given: an email corresponder as recipient
        EmailConfiguration.Corresponder recipient = mockCorresponder("mock-name", "mock-email");

        // and: a collection of template parameters
        Map<String, Object> params = Map.of(
            "param1", "value1",
            "param2", "value2"
        );

        // when: queueing a task
        fixture.queueTask(recipient, TemplateName.USER_REGISTERED, params);

        // then: the task is queued
        ArgumentCaptor<SendEmailTask.Payload> paramCaptor = ArgumentCaptor.forClass(SendEmailTask.Payload.class);
        verify(scheduler).addJob(eq(fixture), paramCaptor.capture());
        SendEmailTask.Payload payload = paramCaptor.getValue();

        // and: the recipient is passed
        assertEquals(recipient.getName(), payload.recipient.getName());
        assertEquals(recipient.getEmail(), payload.recipient.getEmail());

        // and: the template name is passed
        assertEquals(TemplateName.USER_REGISTERED, payload.templateName);

        // and: the template parameters are passed
        assertEquals(params, payload.params);

        // and: NO user id is passed
        assertNull(payload.userId);
    }

    @Test
    public void testApply_WithUserId_NoRecipient() {
        // given: a user to receive the email
        User user = mockUser();
        when(userService.getUser(user.getId())).thenReturn(Optional.of(user));

        // and: a task context with a payload that identifies the recipient user
        SendEmailTask.Payload payload = mockPayload(user.getId());

        // when: the task is applied
        TaskContext<SendEmailTask.Payload> taskContext = new TaskContext<>(payload);
        fixture.apply(taskContext);

        // then: the email service is called to send the email
        ArgumentCaptor<EmailConfiguration.Corresponder> corresponderCaptor =
            ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(sendEmailService).sendEmail(eq(payload.templateName), corresponderCaptor.capture(), paramsCaptor.capture());

        // and: the email recipient is the user
        EmailConfiguration.Corresponder corresponder = corresponderCaptor.getValue();
        assertEquals(user.getPreferredName(), corresponder.getName());
        assertEquals(user.getEmail(), corresponder.getEmail());

        // and: the parameters are passed
        Map<String,Object> params = paramsCaptor.getValue();
        payload.params.forEach((key, value) -> assertEquals(value, params.get(key)));

        // and: the user is passed as a parameter
        assertEquals(user, params.get("user"));
    }

    @Test
    public void testApply_WithRecipient_NoUserId() {
        // given: an email recipient
        SendEmailTask.EmailRecipient recipient =
            new SendEmailTask.EmailRecipient(mockCorresponder("mock-name", "mock-email"));

        // and: a task context with a payload that identifies the recipient user
        SendEmailTask.Payload payload = mockPayload(recipient);

        // when: the task is applied
        TaskContext<SendEmailTask.Payload> taskContext = new TaskContext<>(payload);
        fixture.apply(taskContext);

        // then: the email service is called to send the email
        ArgumentCaptor<EmailConfiguration.Corresponder> corresponderCaptor =
            ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(sendEmailService).sendEmail(eq(payload.templateName), corresponderCaptor.capture(), paramsCaptor.capture());

        // and: the email recipient is the recipient passed in the payload
        EmailConfiguration.Corresponder corresponder = corresponderCaptor.getValue();
        assertEquals(recipient.getName(), corresponder.getName());
        assertEquals(recipient.getEmail(), corresponder.getEmail());

        // and: the parameters are passed
        Map<String,Object> params = paramsCaptor.getValue();
        payload.params.forEach((key, value) -> assertEquals(value, params.get(key)));

        // and: NO user is passed as a parameter
        assertNull(params.get("user"));
    }

    @Test
    public void testApply_WithUserId_AndRecipient() {
        // given: a user identified by the payload
        User user = mockUser();
        when(userService.getUser(user.getId())).thenReturn(Optional.of(user));

        // and: an email recipient
        SendEmailTask.EmailRecipient recipient =
            new SendEmailTask.EmailRecipient(mockCorresponder("mock-name", "mock-email"));

        // and: a task context with a payload that identifies user and recipient
        SendEmailTask.Payload payload = mockPayload(user.getId(), recipient);

        // when: the task is applied
        TaskContext<SendEmailTask.Payload> taskContext = new TaskContext<>(payload);
        fixture.apply(taskContext);

        // then: the email service is called to send the email
        ArgumentCaptor<EmailConfiguration.Corresponder> corresponderCaptor =
            ArgumentCaptor.forClass(EmailConfiguration.Corresponder.class);
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(sendEmailService).sendEmail(eq(payload.templateName), corresponderCaptor.capture(), paramsCaptor.capture());

        // and: the email recipient is the recipient passed in the payload
        EmailConfiguration.Corresponder corresponder = corresponderCaptor.getValue();
        assertEquals(recipient.getName(), corresponder.getName());
        assertEquals(recipient.getEmail(), corresponder.getEmail());

        // and: the parameters are passed
        Map<String,Object> params = paramsCaptor.getValue();
        payload.params.forEach((key, value) -> assertEquals(value, params.get(key)));

        // and: the user is passed as a parameter
        assertEquals(user, params.get("user"));
    }

    @Test
    public void testApply_NoUserId_OrRecipient() {
        // given: a task context with a payload that identifies no user or recipient
        SendEmailTask.Payload payload = mockPayload(null, null);

        // when: the task is applied
        TaskContext<SendEmailTask.Payload> taskContext = new TaskContext<>(payload);
        fixture.apply(taskContext);

        // then: the email service is called to send the email
        // and: no recipient is passed - the template may provide a default
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(sendEmailService).sendEmail(eq(payload.templateName), isNull(), paramsCaptor.capture());

        // and: the parameters are passed
        Map<String,Object> params = paramsCaptor.getValue();
        payload.params.forEach((key, value) -> assertEquals(value, params.get(key)));

        // and: NO user is passed as a parameter
        assertNull(params.get("user"));
    }

    private SendEmailTask.Payload mockPayload(UUID userId) {
        return mockPayload(userId, null);
    }

    private SendEmailTask.Payload mockPayload(SendEmailTask.EmailRecipient recipient) {
        return mockPayload(null, recipient);
    }

    private SendEmailTask.Payload mockPayload(UUID userId, SendEmailTask.EmailRecipient recipient) {
        return SendEmailTask.Payload.builder()
            .userId(userId)
            .recipient(recipient)
            .params(Map.of("param1", "value1", "param2", "value2"))
            .templateName(TemplateName.USER_REGISTERED)
            .build();
    }

    private EmailConfiguration.Corresponder mockCorresponder(String name, String email) {
        return new EmailConfiguration.Corresponder() {
            @Override
            public String getName() { return name; }

            @Override
            public String getEmail() { return email; }

            @Override
            public Optional<Locale> getLocale() { return Optional.empty(); }
        };
    }

    private User mockUser() {
        return User.builder()
            .id(UUID.randomUUID())
            .preferredName("mock-name")
            .email("mock-email")
            .build();
    }
}
