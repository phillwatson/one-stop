package com.hillayes.yapily.service;

import com.hillayes.yapily.api.PaymentsApi;
import com.hillayes.yapily.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@ApplicationScoped
@Slf4j
public class PaymentsService extends AbstractRailService {
    @Inject
    @RestClient
    PaymentsApi paymentsApi;

    public ApiResponseOfPaymentAuthorisationRequestResponse createPaymentAuthorisation(PaymentAuthorisationRequest request) {
        log.debug("Creating payment authorisation [institution: {}, paymentIdempotencyId: {}]",
            request.getInstitutionId(), request.getPaymentRequest().getPaymentIdempotencyId());
        return paymentsApi.createPaymentAuthorisation(request);
    }

    public ApiResponseOfPaymentResponse createPayment(String consentToken,
                                                      PaymentRequest paymentRequest) {
        log.debug("Creating payment [paymentIdempotencyId: {}]", paymentRequest.getPaymentIdempotencyId());
        ApiResponseOfPaymentResponse response = paymentsApi.createPayment(consentToken, paymentRequest);

        log.debug("Payment created [paymentId: {}]", response.getData().getId());
        return response;
    }

    public List<PaymentResponse> getPaymentDetails(String consentToken,
                                                   String paymentId) {
        log.debug("Retrieving payment details [paymentId: {}]", paymentId);
        try {
            PaymentResponses response = paymentsApi.getPaymentDetails(consentToken, paymentId).getData();
            return (response == null) ? List.of() : response.getPayments();
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return List.of();
            }
            throw e;
        }
    }
}
