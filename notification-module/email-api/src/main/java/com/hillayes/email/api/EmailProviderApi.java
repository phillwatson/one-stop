package com.hillayes.email.api;

import com.hillayes.email.api.domain.SendEmail;

/**
 * A bridge to the email service provider API.
 */
public interface EmailProviderApi {
    /**
     * Sends the given sendEmail to the recipients identified within it.
     * The service provider will return an identifier that can be used to track the
     * sendEmail's delivery.
     *
     * @param sendEmail the sendEmail instance to be delivered; with the emailSender and recipients
     *              identifiers, and the content.
     * @return the identifier returned by the sendEmail service provider
     */
    public String sendEmail(SendEmail sendEmail);
}
