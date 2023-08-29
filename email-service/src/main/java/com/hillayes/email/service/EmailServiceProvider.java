package com.hillayes.email.service;

import com.hillayes.email.config.EmailConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import sibApi.TransactionalEmailsApi;

@Slf4j
public class EmailServiceProvider {
    /**
     * Returns an instance of the Email provider's API, initialised with any auth
     * key.
     * @param configuration the configuration that contains the service auth key.
     * @return an initialised instance of the Email provider's API.
     */
    @Produces
    @ApplicationScoped
    public TransactionalEmailsApi getEmailApi(EmailConfiguration configuration) {
        log.debug("Creating Email API instance");
        TransactionalEmailsApi result = new TransactionalEmailsApi();

        result.getApiClient().setApiKey(configuration.apiKey());
        configuration.serviceUrl().ifPresent( url -> {
            log.info("Email service is on {}", url);
            result.getApiClient().setBasePath(url);
        });

        return result;
    }
}
