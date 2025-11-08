package com.hillayes.notification.repository;

import com.hillayes.events.events.audit.AuditIssuesFound;
import com.hillayes.events.events.portfolio.SharesTransacted;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class TemplateRepositoryTest {
    private static final RandomStringUtils randomStrings = RandomStringUtils.insecure();

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

    @ParameterizedTest
    @ValueSource(ints = { 100, -100 })
    public void testSharesTransacted(int quantity) {
        // Given: an event to be processed
        SharesTransacted event = SharesTransacted.builder()
            .userId(UUID.randomUUID())
            .companyName(randomStrings.nextAlphanumeric(20))
            .companyIsin(randomStrings.nextAlphanumeric(12))
            .companyTickerSymbol(randomStrings.nextAlphabetic(4))
            .dateExecuted(LocalDate.now())
            .purchase(quantity > 0)
            .quantity(Math.abs(quantity))
            .price(BigDecimal.valueOf(123.45))
            .build();

        Map<String, Object> params = Map.of("event", event);

        // When: the template is rendered
        String content = fixture.renderTemplate(TemplateName.SHARES_TRANSACTED, params, Optional.empty());

        // Then: the wording is correct
        if (quantity > 0)
            assertTrue(content.contains("We can confirm your purchase of 100 shares"));
        else
            assertTrue(content.contains("We can confirm your sale of 100 shares"));

        // When: the subject is rendered
        String subject = fixture.renderSubject(TemplateName.SHARES_TRANSACTED, params, Optional.empty());

        // Then: the wording is correct
        if (quantity > 0)
            assertTrue(subject.contains("Your purchase of shares"));
        else
            assertTrue(subject.contains("Your sale of shares"));
    }
}
