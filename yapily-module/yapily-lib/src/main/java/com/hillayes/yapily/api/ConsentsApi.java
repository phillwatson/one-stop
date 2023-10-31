package com.hillayes.yapily.api;

import com.hillayes.yapily.model.Consent;
import com.hillayes.yapily.model.OneTimeTokenRequest;
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
public interface ConsentsApi {
    /**
     * Exchange a One-time-token for the consent token.
     *
     * @param request The one time token to exchange for a consent token.
     * @return
     */
    @POST
    @Path("consent-one-time-token")
    public Consent exchangeOneTimeToken(OneTimeTokenRequest request);
}
