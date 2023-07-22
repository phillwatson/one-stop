package com.hillayes.email.repository;

import com.hillayes.email.config.TemplateName;
import com.hillayes.email.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
