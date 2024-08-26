package com.hillayes.notification.repository;

import com.hillayes.events.events.audit.AuditIssuesFound;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class TemplateRepositoryTest {
    @Inject
    TemplateRepository fixture;

    @Test
    public void testRenderSubject_English() {
        // given: a user has been created
        User user = User.builder()
            .preferredName("Jack")
            .build();

        // and: a collection of template parameters
        Map<String, Object> params = Map.of("user", user);

        // when: renderSubject is called
        String subject = fixture.renderSubject(TemplateName.USER_REGISTERED, params, Optional.of(Locale.ENGLISH));

        // then: the subject is rendered in English
        assertEquals("Hi Jack, please complete your One-Stop registration", subject);
    }

    @Test
    public void testRenderSubject_French() {
        // given: a user has been created
        User user = User.builder()
            .preferredName("Jack")
            .build();

        // and: a collection of template parameters
        Map<String, Object> params = Map.of("user", user);

        // when: renderSubject is called
        String subject = fixture.renderSubject(TemplateName.USER_REGISTERED, params, Optional.of(Locale.FRENCH));

        // then: the subject is rendered in French
        assertEquals("Salut Jack, veuillez compléter votre inscription One-Stop", subject);
    }

    @Test
    public void testRenderSubject_UnknownLocale() {
        // given: a user has been created
        User user = User.builder()
            .preferredName("Jack")
            .build();

        // and: a collection of template parameters
        Map<String, Object> params = Map.of("user", user);

        // when: renderSubject is called
        String subject = fixture.renderSubject(TemplateName.USER_REGISTERED, params, Optional.of(Locale.CHINESE));

        // then: the subject is rendered in the default locale (English)
        assertEquals("Hi Jack, please complete your One-Stop registration", subject);
    }

    @Test
    public void testAuditIssuesFound() {
        // given: a user has been created
        User user = User.builder()
            .id(UUID.randomUUID())
            .preferredName("Jack")
            .build();

        // and: a collection of template parameters
        AuditIssuesFound event = AuditIssuesFound.builder()
            .userId(user.getId())
            .dateDetected(Instant.now())
            .issueCounts(Map.of(
                "report 1", 100,
                "report 2", 200,
                "report 3", 300
            )).build();
        Map<String, Object> params = Map.of("event", event);

        // when: renderTemplate is called
        String content = fixture.renderTemplate(TemplateName.AUDIT_ISSUE_FOUND, params, Optional.empty());

        // then: the report list is rendered
        assertTrue(content.contains("<li>\"report 1\" has 100 new issue(s)</li>"));
        assertTrue(content.contains("<li>\"report 2\" has 200 new issue(s)</li>"));
        assertTrue(content.contains("<li>\"report 3\" has 300 new issue(s)</li>"));
    }
}
