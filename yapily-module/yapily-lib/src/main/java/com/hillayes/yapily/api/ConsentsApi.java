package com.hillayes.yapily.api;

import com.hillayes.yapily.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.Instant;
import java.util.List;

@RegisterRestClient(configKey = "yapily-api")
@RegisterClientHeaders(BasicHeaderFactory.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface ConsentsApi {
    /**
     * Exchange a One-time-token for the consent token.
     *
     * @param request The one time token to exchange for a consent token.
     * @return the new consent token.
     */
    @POST
    @Path("/consent-one-time-token")
    public Consent exchangeOneTimeToken(OneTimeTokenRequest request);

    /**
     * Lists the consents.
     *
     * @param appUserIdFilter Filter records based on the list of applicationUserId users provided.
     * @param userIdFilter Filter records based on the list of userUuid users provided.
     * @param institutionFilter Filter records based on the list of Institution provided.
     * @param statusFilter Filter records based on the list of Consent statuses.
     * @param from Returned consents will be on or after this date (yyyy-MM-dd'T'HH:mm:ss.SSSZ).
     * @param before Returned consents will be on or before this date (yyyy-MM-dd'T'HH:mm:ss.SSSZ).
     * @param offset The number of consent records to be skipped. Used primarily with paginated results.
     * @param limit The maximum number of consents records to be returned. Must be between 0 and 1000.
     * @return the list of filtered consents.
     */
    @GET
    @Path("/consents")
    public ApiListResponseOfConsent getConsents(@QueryParam("filter[applicationUserId]") List<String> appUserIdFilter,
                                                @QueryParam("filter[userId]") List<String> userIdFilter,
                                                @QueryParam("filter[institution]") List<String> institutionFilter,
                                                @QueryParam("filter[status]") List<String> statusFilter,
                                                @QueryParam("from") Instant from,
                                                @QueryParam("before") Instant before,
                                                @QueryParam("offset") @DefaultValue("0") int offset,
                                                @QueryParam("limit") int limit);

    /**
     * Get consent information using the consent Id
     *
     * @param consentId the ID of the consent to be retrieved.
     * @return the identified consent information.
     */
    @GET
    @Path("/consents/{consentId}")
    public ApiResponseOfConsent getConsent(@PathParam("consentId") String consentId);

    /**
     * Delete the identified consent.
     *
     * @param consentId the ID of the consent to be retrieved.
     * @param forceDelete Whether to force the deletion.
     * @return the deleted consent information.
     */
    @DELETE
    @Path("/consents/{consentId}")
    public ApiResponseOfConsentDeleteResponse deleteConsent(@PathParam("consentId") String consentId,
                                                            @QueryParam("forceDelete") boolean forceDelete);

    /**
     * Used to indicate to Yapily that reconfirmation has occurred for a given Consent, and to update
     * lastUpdatedAt and reconfirmBy for that Consent.
     *
     * @param consentId the ID of the consent to be extended.
     * @param request the extension requests, specifying the date-time that the user confirmed access
     *     to their account information
     * @return the details of the identified consent after the extension.
     */
    @POST
    @Path("/consents/{consentId}/extend")
    public ApiResponseOfConsent extendConsent(@PathParam("consentId") String consentId,
                                              ExtendConsentRequest request);
}
