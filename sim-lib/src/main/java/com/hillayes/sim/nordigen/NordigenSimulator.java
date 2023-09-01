package com.hillayes.sim.nordigen;

import com.hillayes.nordigen.model.ObtainJwtResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.util.UUID;

@Path(NordigenSimulator.BASE_URI)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class NordigenSimulator {
    public static final String BASE_URI = "/nordigen-sim";

    @Inject
    InstitutionsEndpoint institutionsEndpoint;

    @Inject
    AgreementsEndpoint agreementsEndpoint;

    @Inject
    RequisitionsEndpoint requisitionsEndpoint;

    @Inject
    AccountsEndpoint accountsEndpoint;

    public static NordigenSimClient client(String host) {
        URI baseUri = URI.create(host + BASE_URI);
        return RestClientBuilder.newBuilder()
            .baseUri(baseUri)
            .build(NordigenSimClient.class);
    }

    public NordigenSimulator() {
        log.info("Started Nordigen simulator");
    }

    /**
     * Resets the simulator to its initial state. Typically called before each test.
     */
    @PUT
    @Path("/reset")
    public void reset() {
        log.info("Resetting Nordigen simulator state");
        institutionsEndpoint.reset();
        agreementsEndpoint.reset();
        requisitionsEndpoint.reset();
        accountsEndpoint.reset();
    }

    /**
     * Mocks the endpoint to obtain access and refresh tokens from Nordigen.
     */
    @POST
    @Path("/api/v2/token/new/")
    public Response login() {
        ObtainJwtResponse response = ObtainJwtResponse.builder()
            .access(UUID.randomUUID().toString())
            .accessExpires(3600)
            .refresh(UUID.randomUUID().toString())
            .refreshExpires(7200)
            .build();

        return Response.ok(response).build();
    }
}
