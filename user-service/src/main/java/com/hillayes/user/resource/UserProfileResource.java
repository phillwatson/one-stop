package com.hillayes.user.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.UUID;

@Path("/api/v1/profiles")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserProfileResource {
    private final UserService userService;

    @GET
    public Response getUser(@Context SecurityContext ctx) {
        UUID id = getUserId(ctx);
        log.info("Getting user profile [id: {}]", id);

        return userService.getUser(id)
            .map(user -> Response.ok(user).build())
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @PUT
    public Response updateUser(@Context SecurityContext ctx,
                               User userUpdate) {
        UUID id = getUserId(ctx);
        log.info("Update user profile [id: {}]", id);
        return userService.updateUser(id, userUpdate)
            .map(user -> {
                log.debug("Updated user [username: {}, id: {}]", user.getUsername(), user.getId());
                return Response.ok(user).build();
            })
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    private UUID getUserId(SecurityContext context) {
        return UUID.fromString(context.getUserPrincipal().getName());
    }
}
