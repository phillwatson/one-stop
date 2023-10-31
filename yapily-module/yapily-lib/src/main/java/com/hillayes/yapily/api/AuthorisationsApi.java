package com.hillayes.yapily.api;

import com.hillayes.yapily.model.ApiResponseOfPaymentAuthorisationRequestResponse;
import com.hillayes.yapily.model.PaymentAuthorisationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "yapily-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface AuthorisationsApi {
    /**
     * Used to initiate the authorisation process and direct users to the login screen of their
     * financial Institution in order to give their consent for a payment. This endpoint is used
     * to initiate all the different payment listed below. Based on the type of payment you wish
     * to make, you may be required to provide specific properties in PaymentRequest. First make
     * sure that the payment feature you wish to execute is supported by the bank by checking
     * the features array in GET Institution.
     *
     * @param request
     * @return
     */
    @POST
    @Path("payment-auth-requests")
    public ApiResponseOfPaymentAuthorisationRequestResponse createPaymentAuthorisation(PaymentAuthorisationRequest request);
}
