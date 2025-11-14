package com.hillayes.email.api;

import com.hillayes.email.api.domain.SendEmail;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Priority(-1)
@Slf4j
public class NullEmailProviderApi implements EmailProviderApi {
    @Override
    public String sendEmail(SendEmail sendEmail) {
        log.warn("NULL EmailProvider sending sendEmail [recipient: {}, subject: {}]",
            sendEmail.getTo().getFirst(), sendEmail.getSubject());
        return "";
    }
}
