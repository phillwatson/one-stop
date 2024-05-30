package com.hillayes.yapily.api;

import com.hillayes.yapily.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.Instant;

@RegisterRestClient(configKey = "yapily-api")
@RegisterClientHeaders(BasicHeaderFactory.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface AccountsApi {
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

    /**
     * Get accounts.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @return the list of accounts.
     */
    @GET
    @Path("/accounts")
    public AccountApiListResponse getAccounts(@HeaderParam("consent") String consentToken);

    /**
     * Get account.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @param accountId The account Id of the user's bank account.
     * @return the identified account.
     */
    @GET
    @Path("/accounts/{accountId}")
    public ApiResponseOfAccount getAccount(@HeaderParam("consent") String consentToken,
                                           @PathParam("accountId") String accountId);

    /**
     * Returns the identity information for an account.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @return Identification details of a party associated with an account e.g. (account owner or operator).
     */
    @GET
    @Path("/identity")
    public ApiResponseOfIdentity getIdentity(@HeaderParam("consent") String consentToken);

    /**
     * Returns the balance for the end user associated with the presented consent token.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @param accountId The account Id of the user's bank account.
     * @return the balance for the end user associated with the presented consent token.
     */
    @GET
    @Path("/accounts/{accountId}/balances")
    public ApiResponseOfBalances getBalances(@HeaderParam("consent") String consentToken,
                                             @PathParam("accountId") String accountId);

    /**
     * Returns all the beneficiaries of a user's account.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @param accountId The account Id of the user's bank account.
     * @return all the beneficiaries of a user's account.
     */
    @GET
    @Path("/accounts/{accountId}/beneficiaries")
    public ApiListResponseOfBeneficiary getBeneficiaries(@HeaderParam("consent") String consentToken,
                                                         @PathParam("accountId") String accountId);

    /**
     * Returns the list of direct debits for an account.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @param accountId The account Id of the user's bank account.
     * @return the list of direct debits for an account.
     */
    @GET
    @Path("/accounts/{accountId}/direct-debits")
    public ApiListResponseOfDirectDebitResponse getDirectDebits(@HeaderParam("consent") String consentToken,
                                                                @PathParam("accountId") String accountId);

    /**
     * Returns the list of periodic payments (standing orders in the UK) for an account.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @param accountId The account Id of the user's bank account.
     * @return the list of periodic payments (standing orders in the UK) for an account.
     */
    @GET
    @Path("/accounts/{accountId}/periodic-payments")
    public ApiListResponseOfPaymentResponse getPeriodicPayments(@HeaderParam("consent") String consentToken,
                                                                @PathParam("accountId") String accountId);

    /**
     * Returns the list of scheduled payments for an account.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @param accountId The account Id of the user's bank account.
     * @return the list of scheduled payments for an account.
     */
    @GET
    @Path("/accounts/{accountId}/scheduled-payments")
    public ApiListResponseOfPaymentResponse getScheduledPayments(@HeaderParam("consent") String consentToken,
                                                                 @PathParam("accountId") String accountId);

    /**
     * Returns the list of statements for an account.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @param accountId The account Id of the user's bank account.
     * @param from Returned statements will be on or after this date (yyyy-MM-dd'T'HH:mm:ss.SSSZ).
     * @param before Returned statements will be on or before this date (yyyy-MM-dd'T'HH:mm:ss.SSSZ).
     * @param offset The number of statement records to be skipped. Used primarily with paginated results.
     * @param limit The maximum number of statement records to be returned. Must be between 0 and 1000.
     * @param sortBy Sort statement records by date ascending with 'date' or descending with '-date'.
     *     The default sort order is descending. Available values : date, -date
     * @return the list of statements for an account.
     */
    @GET
    @Path("/accounts/{accountId}/statements")
    public ApiListResponseOfAccountStatement getStatements(@HeaderParam("consent") String consentToken,
                                                           @PathParam("accountId") String accountId,
                                                           @QueryParam("from") Instant from,
                                                           @QueryParam("before") Instant before,
                                                           @QueryParam("offset") int offset,
                                                           @QueryParam("limit") int limit,
                                                           @QueryParam("sort") String sortBy);

    /**
     * Returns a statement for an account.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @param accountId The account Id of the user's bank account.
     * @param statementId The statement Id of the statement file.
     * @return a statement for an account.
     */
    @GET
    @Path("/accounts/{accountId}/statements/{statementId}")
    public ApiResponseOfAccountStatement getStatement(@HeaderParam("consent") String consentToken,
                                                      @PathParam("accountId") String accountId,
                                                      @PathParam("statementId") String statementId);

    /**
     * Returns the account transactions for an account.
     *
     * @param consentToken The `consent-token` containing the user's authorisation to make the request.
     * @param accountId The account Id of the user's bank account.
     * @param categorisation Acceptable value: `categorisation`. When set, will include enrichment data
     *     in the transactions returned. Enrichment data is optional, e.g. when 'categorisation'
     *     enrichment data is requested, the enrichment response will include categorisation data and
     *     merchant name, only if it can be evaluated from the transaction. This service is limited for UK
     *     institution transactions currently.
     * @param from Returned transactions will be on or after this date (yyyy-MM-dd'T'HH:mm:ss.SSSZ).
     * @param before Returned transactions will be on or before this date (yyyy-MM-dd'T'HH:mm:ss.SSSZ).
     * @param offset The number of transaction records to be skipped. Used primarily with paginated results.
     * @param limit The maximum number of transaction records to be returned. Must be between 0 and 1000.
     * @param sortBy Sort transaction records by date ascending with 'date' or descending with '-date'.
     *     The default sort order is descending Enum: "date" "-date"
     * @return the account transactions for an account.
     */
    @GET
    @Path("/accounts/{accountId}/transactions")
    public ApiListResponseOfTransaction getTransactions(@HeaderParam("consent") String consentToken,
                                                        @PathParam("accountId") String accountId,
                                                        @QueryParam("from") Instant from,
                                                        @QueryParam("before") Instant before,
                                                        @QueryParam("offset") int offset,
                                                        @QueryParam("limit") int limit,
                                                        @QueryParam("with") String categorisation,
                                                        @QueryParam("sort") String sortBy);
}
