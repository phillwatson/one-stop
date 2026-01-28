package com.hillayes.shares.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Path("/api/v1/shares/dealings/{dealingId}")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class DealingsResource {
    @GET
    public Response getShareDealing(@Context SecurityContext ctx,
                                    @PathParam("dealingId") UUID dealingId) {
        return Response.ok().build();
    }

    @PUT
    public Response updateShareDealing(@Context SecurityContext ctx,
                                       @PathParam("dealingId") UUID dealingId) {
        return Response.ok().build();
    }

    @DELETE
    public Response deleteShareDealing(@Context SecurityContext ctx,
                                       @PathParam("dealingId") UUID dealingId) {
        return Response.ok().build();
    }
}
