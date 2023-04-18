package com.hillayes.email.service;

import com.hillayes.email.EmailConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import sendinblue.ApiException;

import javax.inject.Inject;

@QuarkusTest
public class SendEmailServiceTest {
    @Inject
    EmailConfiguration emailConfiguration;

    @Inject
    SendEmailService fixture;

    @Test
    public void testSendEmail() throws ApiException {
        fixture.sendEmail(emailConfiguration.templates().get("user-created"), null);
    }
}
