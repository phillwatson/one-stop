package com.hillayes.email.brevo.api;

import com.hillayes.email.brevo.api.domain.BrevoEmail;
import com.hillayes.email.brevo.api.domain.BrevoEmailResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
@RegisterRestClient(configKey = "brevo-api")
@RegisterClientHeaders(ApiKeyHeaderFactory.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SendEmailApi {
    @POST
    @Path("/smtp/email")
    public BrevoEmailResponse sendEmail(BrevoEmail email);
}
