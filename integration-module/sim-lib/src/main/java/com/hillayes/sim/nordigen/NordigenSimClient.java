package com.hillayes.sim.nordigen;

import com.hillayes.nordigen.model.RequisitionStatus;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Allows tests to interact with the internal state of the Nordigen simulator.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface NordigenSimClient {
    /**
     * Resets the simulator to its initial state. Typically called before each test.
     */
    @PUT
    @Path("/reset")
    public Response reset();

    /**
     * Allows a test to update the status of a requisition; to simulate the requisition's
     * status changing in the Nordigen system.
     * @param id the requisition's id
     * @param status the new status
     * @return the response
     */
    @PUT
    @Path("/api/v2/requisitions/{id}/")
    public Response updateRequisitionStatus(@PathParam("id") String id,
                                            @QueryParam("status") RequisitionStatus status);
}
