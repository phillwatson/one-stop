package com.hillayes.sim.nordigen;

import com.hillayes.nordigen.model.RequisitionStatus;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface NordigenSimClient {
    @PUT
    @Path("/reset")
    public Response reset();

    @PUT
    @Path("/api/v2/requisitions/{id}/")
    public Response updateRequisitionStatus(@PathParam("id") String id,
                                            @QueryParam("status") RequisitionStatus status);
}
