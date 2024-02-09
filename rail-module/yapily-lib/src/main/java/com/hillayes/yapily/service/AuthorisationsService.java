package com.hillayes.yapily.service;

import com.hillayes.yapily.api.AccountsApi;
import com.hillayes.yapily.api.AuthorisationsApi;
import com.hillayes.yapily.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class AuthorisationsService extends AbstractRailService {
    @Inject
    @RestClient
    AuthorisationsApi authorisationsApi;

    public ApiResponseOfPaymentAuthorisationRequestResponse createPaymentAuthorisation(PaymentAuthorisationRequest request) {
        log.debug("Creating payment authorisation [institution: {}]", request.getInstitutionId());
        return authorisationsApi.createPaymentAuthorisation(request);
    }

    public ApiResponseOfAccountAuthorisationResponse createAccountAuthorisation(AccountAuthorisationRequest request) {
        log.debug("Creating account authorisation [institution: {}]", request.getInstitutionId());
        return authorisationsApi.createAccountAuthorisation(request);
    }
}
