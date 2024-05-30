package com.hillayes.yapily.api;

import com.hillayes.yapily.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "yapily-api")
@RegisterClientHeaders(BasicHeaderFactory.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface PaymentsApi {
    /**
     * Used to initiate the authorisation process and direct users to the login screen of their
     * financial Institution in order to give their consent for a payment. This endpoint is used
     * to initiate all the different payments. Based on the type of payment you wish to make,
     * you may be required to provide specific properties in PaymentRequest. First make sure that
     * the payment feature you wish to execute is supported by the bank by checking the features
     * array in InstitutionsApi.getInstitution(String).
     *
     * @param request
     * @return
     */
    @POST
    @Path("/payment-auth-requests")
    public ApiResponseOfPaymentAuthorisationRequestResponse createPaymentAuthorisation(PaymentAuthorisationRequest request);

    /**
     * Creates a payment after obtaining the user's authorisation.
     *
     * @param consentToken The consent-token containing the user's authorisation to make the request.
     * @param paymentRequest
     * @return
     */
    @POST
    @Path("/payments")
    public ApiResponseOfPaymentResponse createPayment(@HeaderParam("consent") String consentToken,
                                                      PaymentRequest paymentRequest);

    /**
     * Returns the details of a payment.
     * Most commonly used to check for payment status updates.
     *
     * @param consentToken The consent-token containing the user's authorisation to make the request.
     * @param paymentId The payment Id of the payment.
     * @return
     */
    @GET
    @Path("/payments/{paymentId}/details")
    public ApiResponseOfPaymentResponses getPaymentDetails(@HeaderParam("consent") String consentToken,
                                                           @PathParam("paymentId") String paymentId);
}
