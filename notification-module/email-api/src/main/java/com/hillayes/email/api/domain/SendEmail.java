package com.hillayes.email.api.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Encapsulates the email properties that should be supported by the SendEmail Service Provdier.
 */
@Builder(builderClassName = "Builder")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SendEmail {
    private EmailSender sender;

    @lombok.Builder.Default
    private List<EmailRecipient> to = List.of();

    @lombok.Builder.Default
    private List<EmailRecipient> bcc = List.of();

    @lombok.Builder.Default
    private List<EmailRecipient> cc = List.of();

    private String htmlContent;

    private String subject;
}
