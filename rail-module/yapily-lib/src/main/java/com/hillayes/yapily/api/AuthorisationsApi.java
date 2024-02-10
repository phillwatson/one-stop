package com.hillayes.yapily.api;

import com.hillayes.yapily.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "yapily-api")
@RegisterClientHeaders(BasicHeaderFactory.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface AuthorisationsApi {
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
     * Used to initiate the authorisation process and direct users to the login screen of their
     * financial institution in order to give consent to access account data.
     *
     * <ol>
     *     <li>Select institution</li>
     *     <li>Create Account Authorisation</li>
     *     <li>Record the auth request in a database record</li>
     *     <li>Generate callback URL with DB record id</li>
     *     <li>Redirect user to the authorisation URL in the response - incl callback URL</li>
     *     <li>Listen on callback URL</li>
     *     <li>Looking up auth request record using ID in the callback url</li>
     *     <li>Record the consent token from the callback body</li>
     * </ol>
     *
     * @param request
     * @return
     */
    @POST
    @Path("/account-auth-requests")
    public ApiResponseOfAccountAuthorisationResponse createAccountAuthorisation(AccountAuthorisationRequest request);
}
