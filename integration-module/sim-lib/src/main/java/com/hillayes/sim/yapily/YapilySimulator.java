package com.hillayes.sim.yapily;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;

@Path(YapilySimulator.BASE_URI)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class YapilySimulator {
    public static final String BASE_URI = "/yapily-sim";

    @Inject
    InstitutionsEndpoint institutionsEndpoint;

    public static YapilySimClient client(String host) {
        URI baseUri = URI.create(host + BASE_URI);
        return RestClientBuilder.newBuilder()
            .baseUri(baseUri)
            .build(YapilySimClient.class);
    }

    public YapilySimulator() {
        log.info("Started Yapily simulator");
    }

    /**
     * Resets the simulator to its initial state. Typically called before each test.
     */
    @PUT
    @Path("/reset")
    public void reset() {
        log.info("Resetting Yapily simulator state");
        institutionsEndpoint.reset();
    }
}
