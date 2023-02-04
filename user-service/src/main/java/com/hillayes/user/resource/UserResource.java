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
import java.util.Collection;
import java.util.UUID;

@Path("/api/v1/users/self")
@RolesAllowed({"admin", "user"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserResource {
    private final UserService userService;

    @GET
    public Response getUser(@Context SecurityContext ctx) {
        UUID id = getUserId(ctx);
        log.info("Getting user [id: {}]", id);

        return userService.getUser(id)
            .map(user -> Response.ok(user).build())
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @GET
    @Path("/roles")
    public Response getUserRoles(@Context SecurityContext ctx) {
        UUID id = getUserId(ctx);
        log.info("Getting user roles [id: {}]", id);

        Collection<String> roles = userService.getUserRoles(id);
        if (roles.isEmpty()) {
            throw new NotFoundException("user", id);
        }

        return Response.ok(roles).build();
    }

    @PUT
    public Response updateUser(@Context SecurityContext ctx,
                               User userUpdate) {
        UUID id = getUserId(ctx);
        log.info("Update user [id: {}]", id);
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
