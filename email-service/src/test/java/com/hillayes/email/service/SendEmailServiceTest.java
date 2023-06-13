package com.hillayes.email.service;

import com.hillayes.email.config.EmailConfiguration;
import com.hillayes.email.config.TemplateName;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

@QuarkusTest
public class SendEmailServiceTest {
    @Inject
    EmailConfiguration emailConfiguration;

    @Inject
    SendEmailService fixture;

    @Test
    public void testSendEmail() throws Exception {
        fixture.sendEmail(TemplateName.USER_CREATED, null, null);
    }
}
