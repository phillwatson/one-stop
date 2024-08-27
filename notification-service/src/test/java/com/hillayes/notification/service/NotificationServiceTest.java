package com.hillayes.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.jpa.Page;
import com.hillayes.events.events.audit.AuditIssuesFound;
import com.hillayes.events.events.consent.ConsentExpired;
import com.hillayes.events.events.consent.ConsentSuspended;
import com.hillayes.notification.config.NotificationConfiguration;
import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationId;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class NotificationServiceTest {
    @Spy
    ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    UserService userService;

    @Mock
    NotificationConfiguration configuration;

    @InjectMocks
    NotificationService fixture;

    @BeforeEach
    public void setup() {
        openMocks(this);
    }

    @BeforeEach
    public void beforeEach() {
        when(notificationRepository.save(any())).then(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) {
                notification = notification.toBuilder().id(UUID.randomUUID()).build();
            }
            return notification;
        });
    }

    @Test
    public void testCreateNotification() throws JsonProcessingException {
        // given: a user ID
        UUID userId = UUID.randomUUID();

        // and: a notification ID
        NotificationId notificationId = NotificationId.CONSENT_EXPIRED;

        // and: a parameter map containing an event payload
        ConsentExpired event = mockConsentExpired();

        // when: the service is called
        Notification notification = fixture.createNotification(userId, Instant.now(),
            notificationId, Map.of("event", event));

        // then: the notification is saved
        verify(notificationRepository).save(any());

        // and: a notification is created
        assertNotNull(notification);

        // and: the notification reflects given IDs
        assertEquals(userId, notification.getUserId());
        assertEquals(notificationId, notification.getMessageId());

        // and: the parameters are mapped to JSON
        assertNotNull(notification.getAttributes());

        // when: the parameters are deserialized
        HashMap<String, Object> params = objectMapper.readValue(notification.getAttributes(), HashMap.class);

        // then: the event payload is returned
        Map<String, Object> eventMap = (Map<String, Object>) params.get("event");
        assertNotNull(eventMap);

        // and: the event payload matches the input data
        assertEquals(event.getUserId().toString(), eventMap.get("userId"));
        assertEquals(event.getConsentId().toString(), eventMap.get("consentId"));
        assertEquals(event.getInstitutionId(), eventMap.get("institutionId"));
        assertEquals(event.getInstitutionName(), eventMap.get("institutionName"));
        assertEquals(event.getAgreementId(), eventMap.get("agreementId"));
        assertEquals(event.getAgreementExpires(), Instant.parse((String) eventMap.get("agreementExpires")));
        assertEquals(event.getDateExpired(), Instant.parse((String) eventMap.get("dateExpired")));
    }

    @Test
    public void testListNotifications_HappyPath() throws JsonProcessingException {
        // given: a user
        User user = User.builder()
            .id(UUID.randomUUID())
            .givenName(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        when(userService.getUser(user.getId()))
            .thenReturn(Optional.of(user));

        // and: an available list of notifications for consent events
        ConsentSuspended consentSuspended = mockConsentSuspended();
        ConsentExpired consentExpired = mockConsentExpired();
        List<Notification> notifications = List.of(
            mockNotification(NotificationId.CONSENT_SUSPENDED, Map.of("event", consentSuspended)),
            mockNotification(NotificationId.CONSENT_EXPIRED, Map.of("event", consentExpired))
        );
        when(notificationRepository.listByUserAndTime(eq(user.getId()), any(), anyInt(), anyInt()))
            .thenReturn(Page.of(notifications));

        // and: a date-time from which to start
        Instant after = Instant.now();

        // and: a NotificationMapper implementation - to assert results and return rendered messages
        NotificationService.NotificationMapper<String> mapper = mockNotificationMapper(notifications);

        // and: a configured notification message
        Map<NotificationId, NotificationConfiguration.MessageConfig> messageConfigs = Map.of(
            NotificationId.CONSENT_SUSPENDED, mockMessageConfig("Access to $event.institutionName$ has been suspended.\nYou need to renew your consent."),
            NotificationId.CONSENT_EXPIRED, mockMessageConfig("Access to $event.institutionName$ has expired.\nYou need to renew your consent.")
        );
        when(configuration.templates()).thenReturn(messageConfigs);

        // when: the service is called
        Page<String> messages = fixture.listNotifications(user.getId(), after, 0, 1000, mapper);

        // then: the user is read from repository
        verify(userService).getUser(user.getId());

        // and: the notifications are read from repository
        verify(notificationRepository).listByUserAndTime(user.getId(), after, 0, 1000);

        // and: the messages are rendered
        assertNotNull(messages);
        assertEquals(notifications.size(), messages.getContentSize());

        // and: the messages passed through the template parameters
        List<String> content = messages.getContent();
        assertEquals("Access to " + consentSuspended.getInstitutionName() + " has been suspended.\nYou need to renew your consent.", content.get(0));
        assertEquals("Access to " + consentExpired.getInstitutionName() + " has expired.\nYou need to renew your consent.", content.get(1));
    }

    @Test
    public void testListNotifications_UserNotFound() throws JsonProcessingException {
        // given: a user cannot be found by ID
        UUID userId = UUID.randomUUID();
        when(userService.getUser(userId))
            .thenReturn(Optional.empty());

        // and: an available list of notifications for consent events
        ConsentSuspended consentSuspended = mockConsentSuspended();
        ConsentExpired consentExpired = mockConsentExpired();
        List<Notification> notifications = List.of(
            mockNotification(NotificationId.CONSENT_SUSPENDED, Map.of("event", consentSuspended)),
            mockNotification(NotificationId.CONSENT_EXPIRED, Map.of("event", consentExpired))
        );
        when(notificationRepository.listByUserAndTime(eq(userId), any(), anyInt(), anyInt()))
            .thenReturn(Page.of(notifications));

        // and: a date-time from which to start
        Instant after = Instant.now();

        // and: a NotificationMapper implementation - to assert results and return rendered messages
        NotificationService.NotificationMapper<String> mapper = mockNotificationMapper(notifications);

        // when: the service is called
        Page<String> messages = fixture.listNotifications(userId, after, 0, 1000, mapper);

        // then: the user is read from repository
        verify(userService).getUser(userId);

        // and: the notifications are NOT read from repository
        verifyNoInteractions(notificationRepository);

        // and: NO messages are rendered
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testDeleteNotification_HappyPath() throws JsonProcessingException {
        // given: a user
        User user = User.builder()
            .id(UUID.randomUUID())
            .givenName(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();

        // and: a notification to be deleted
        Notification notification = mockNotification(user.getId(), NotificationId.CONSENT_SUSPENDED, Map.of());
        when(notificationRepository.findByIdOptional(notification.getId()))
            .thenReturn(Optional.of(notification));

        fixture.deleteNotification(user.getId(), notification.getId());

        // then: the notifications are read from repository
        verify(notificationRepository).findByIdOptional(notification.getId());

        // and: the notification is deleted
        verify(notificationRepository).delete(notification);
    }

    @Test
    public void testDeleteNotification_WrongUserId() throws JsonProcessingException {
        // given: a user
        User user = User.builder()
            .id(UUID.randomUUID())
            .givenName(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();

        // and: a notification, for that user, to be deleted
        Notification notification = mockNotification(user.getId(), NotificationId.CONSENT_SUSPENDED, Map.of());
        when(notificationRepository.findByIdOptional(notification.getId()))
            .thenReturn(Optional.of(notification));

        // when: the service is called
        fixture.deleteNotification(UUID.randomUUID(), notification.getId());

        // then: the notifications are read from repository
        verify(notificationRepository).findByIdOptional(notification.getId());

        // and: NO notification is deleted
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    public void testDeleteNotification_NotFound() throws JsonProcessingException {
        // given: a user
        User user = User.builder()
            .id(UUID.randomUUID())
            .givenName(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();

        // and: a notification ID
        UUID notificationId = UUID.randomUUID();

        // and: the notification cannot be found
        when(notificationRepository.findByIdOptional(notificationId))
            .thenReturn(Optional.empty());

        // when: the service is called
        fixture.deleteNotification(user.getId(), notificationId);

        // and: the notifications are read from repository
        verify(notificationRepository).findByIdOptional(notificationId);

        // and: NO notification is deleted
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    public void testAuditIssueFound() throws JsonProcessingException {
        // given: a user
        User user = User.builder()
            .id(UUID.randomUUID())
            .givenName(randomAlphanumeric(30))
            .email(randomAlphanumeric(30))
            .build();
        when(userService.getUser(user.getId()))
            .thenReturn(Optional.of(user));

        // and: an available list of notifications for consent events
        AuditIssuesFound event = AuditIssuesFound.builder()
            .userId(user.getId())
            .dateDetected(Instant.now())
            .issueCounts(Map.of(
                "report 1", 100,
                "report 2", 200,
                "report 3", 300
            )).build();
        List<Notification> notifications = List.of(
            mockNotification(NotificationId.AUDIT_ISSUE_FOUND, Map.of("event", event))
        );
        when(notificationRepository.listByUserAndTime(eq(user.getId()), any(), anyInt(), anyInt()))
            .thenReturn(Page.of(notifications));

        // and: a date-time from which to start
        Instant after = Instant.now();

        // and: a NotificationMapper implementation - to assert results and return rendered messages
        NotificationService.NotificationMapper<String> mapper = mockNotificationMapper(notifications);

        // and: a configured notification message
        Map<NotificationId, NotificationConfiguration.MessageConfig> messageConfigs = Map.of(
            NotificationId.AUDIT_ISSUE_FOUND, mockMessageConfig(
                "An issue has been found with transactions your account.\n" +
                "$event.issueCounts.keys: { key | $key$:  $event.issueCounts.(key)$ new issues found }; separator=\"\n\"$" +
                "\nPlease contact us."
            )
        );
        when(configuration.templates()).thenReturn(messageConfigs);

        // when: the service is called
        Page<String> messages = fixture.listNotifications(user.getId(), after, 0, 1000, mapper);

        // then: the user is read from repository
        verify(userService).getUser(user.getId());

        // and: the notifications are read from repository
        verify(notificationRepository).listByUserAndTime(user.getId(), after, 0, 1000);

        // and: the messages are rendered
        assertNotNull(messages);
        assertEquals(notifications.size(), messages.getContentSize());

        // and: the messages passed through the template parameters
        String content = messages.getContent().get(0);
        assertTrue(content.contains("report 1:  100 new issues found"));
        assertTrue(content.contains("report 2:  200 new issues found"));
        assertTrue(content.contains("report 3:  300 new issues found"));
    }

    /**
     * An implementation of NotificationService.NotificationMapper that verifies the results
     * returned from the service are as expected, and returns the rendered messages.
     */
    private NotificationService.NotificationMapper mockNotificationMapper(List<Notification> expectedNotifications) {
        final AtomicInteger messageCount = new AtomicInteger();
        return (notification, message) -> {
            int index = messageCount.getAndIncrement();
            Notification expected = expectedNotifications.get(index);
            assertEquals(expected.getId(), notification.getId());
            assertEquals(expected.getUserId(), notification.getUserId());
            assertEquals(expected.getMessageId(), notification.getMessageId());
            return message;
        };
    }

    private Notification mockNotification(NotificationId notificationId,
                                          Map<String, Object> attributes) throws JsonProcessingException {
        return mockNotification(UUID.randomUUID(), notificationId, attributes);
    }

    private Notification mockNotification(UUID userId, NotificationId notificationId,
                                          Map<String, Object> attributes) throws JsonProcessingException {
        return Notification.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .dateCreated(Instant.now())
            .messageId(notificationId)
            .attributes(objectMapper.writeValueAsString(attributes))
            .build();
    }

    private ConsentExpired mockConsentExpired() {
        return ConsentExpired.builder()
            .userId(UUID.randomUUID())
            .consentId(UUID.randomUUID())
            .institutionId(randomAlphanumeric(30))
            .institutionName(randomAlphanumeric(30))
            .dateExpired(Instant.now())
            .agreementId(randomAlphanumeric(30))
            .agreementExpires(Instant.now().plus(Duration.ofDays(30)))
            .build();
    }

    private ConsentSuspended mockConsentSuspended() {
        return ConsentSuspended.builder()
            .userId(UUID.randomUUID())
            .consentId(UUID.randomUUID())
            .institutionId(randomAlphanumeric(30))
            .institutionName(randomAlphanumeric(30))
            .dateSuspended(Instant.now())
            .agreementId(randomAlphanumeric(30))
            .agreementExpires(Instant.now().plus(Duration.ofDays(30)))
            .build();
    }

    private NotificationConfiguration.MessageConfig mockMessageConfig(String message) {
        return () -> Map.of(Locale.ENGLISH, message);
    }
}
