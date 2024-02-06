package com.hillayes.sim.yapily;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Allows tests to interact with the internal state of the Yapily simulator.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface YapilySimClient {
    /**
     * Resets the simulator to its initial state. Typically called before each test.
     */
    @PUT
    @Path("/reset")
    public Response reset();
}
