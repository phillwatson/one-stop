package com.hillayes.shares.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Path("/api/v1/shares/holdings/{holdingId}")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class HoldingsResource {
    @GET
    public Response getShareHolding(@Context SecurityContext ctx,
                                    @PathParam("holdingId") UUID holdingId) {
        return Response.ok().build();
    }

    @DELETE
    public Response deleteShareHolding(@Context SecurityContext ctx,
                                       @PathParam("holdingId") UUID holdingId) {
        return Response.ok().build();
    }

    @POST
    public Response recordShareDealing(@Context SecurityContext ctx,
                                       @PathParam("holdingId") UUID holdingId) {
        return Response.ok().build();
    }

    @GET
    @Path("/dealings")
    public Response getShareDealings(@Context SecurityContext ctx,
                                     @Context UriInfo uriInfo,
                                     @PathParam("holdingId") UUID holdingId,
                                     @QueryParam("page")@DefaultValue("0") int pageIndex,
                                     @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        return Response.ok().build();
    }
}
