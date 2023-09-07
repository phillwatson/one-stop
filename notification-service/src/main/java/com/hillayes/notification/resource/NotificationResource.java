package com.hillayes.notification.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

@Path("/api/v1/notifications")
@RolesAllowed({"user"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class NotificationResource {
    @GET
    public Response getNotifications(@Context SecurityContext ctx,
                                     @QueryParam("after") Instant after) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Getting user notifications [userId: {}, after: {}]", userId, after);

        return Response.ok().build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteNotification(@Context SecurityContext ctx,
                                       @PathParam("id") UUID notificationId) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Deleting user notification [userId: {}, notificationId: {}]", userId, notificationId);

        return Response.ok().build();
    }
}
