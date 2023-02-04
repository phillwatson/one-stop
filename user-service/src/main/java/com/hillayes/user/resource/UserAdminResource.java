package com.hillayes.user.resource;

import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;

@Path("/api/v1/admin/users")
@RolesAllowed({"admin"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserAdminResource {
    private final UserService userService;

    @GET
    public Response listUsers() {
        log.info("Getting users");
        return Response.ok(userService.listUsers()).build();
    }

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") UUID id) {
        log.info("Getting user [id: {}]", id);

        return userService.getUser(id)
            .map(user -> Response.ok(user).build())
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @GET
    @Path("/{id}/roles")
    public Response getUserRoles(@PathParam("id") UUID id) {
        log.info("Getting user roles [id: {}]", id);

        Collection<String> roles = userService.getUserRoles(id);
        if (roles.isEmpty()) {
            throw new NotFoundException("user", id);
        }

        return Response.ok(roles).build();
    }

    @POST
    public Response createUser(User user) {
        if (user == null) {
            throw new MissingParameterException("user content");
        }

        log.info("Creating user [username: {}]", user.getUsername());
        User result = userService.createUser(user.getUsername(), user.getPasswordHash().toCharArray(), user);

        log.debug("Created user [username: {}, id: {}]", result.getUsername(), result.getId());
        return Response.created(URI.create("/api/v1/admin/users/" + result.getId())).build();
    }

    @PUT
    @Path("/{id}/onboard")
    public Response onboardUser(@PathParam("id") UUID id) {
        log.info("Onboard user [id: {}]", id);
        return userService.onboardUser(id)
            .map(user -> {
                log.debug("Onboarded user [username: {}, id: {}]", user.getUsername(), user.getId());
                return Response.ok(user).build();
            })
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @PUT
    @Path("/{id}")
    public Response updateUser(@PathParam("id") UUID id,
                               User userUpdate) {
        log.info("Update user [id: {}]", id);
        return userService.updateUser(id, userUpdate)
            .map(user -> {
                log.debug("Updated user [username: {}, id: {}]", user.getUsername(), user.getId());
                return Response.ok(user).build();
            })
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") UUID id) {
        log.info("Delete user [id: {}]", id);
        return userService.deleteUser(id)
            .map(user -> {
                log.debug("Deleted user [username: {}, id: {}]", user.getUsername(), user.getId());
                return Response.ok().build();
            })
            .orElseThrow(() -> new NotFoundException("user", id));
    }
}
