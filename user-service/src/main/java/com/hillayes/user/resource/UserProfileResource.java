package com.hillayes.user.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.PasswordUpdateRequest;
import com.hillayes.onestop.api.UserProfileRequest;
import com.hillayes.onestop.api.UserProfileResponse;
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
            .map(user -> {
                log.debug("Found user [username: {}, id: {}]", user.getUsername(), user.getId());
                return marshal(user);
            })
            .map(user -> Response.ok(user).build())
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @PUT
    public Response updateUser(@Context SecurityContext ctx,
                               UserProfileRequest userProfileRequest) {
        UUID id = getUserId(ctx);
        log.info("Update user profile [id: {}]", id);

        User userUpdate = User.builder()
            .preferredName(userProfileRequest.getPreferredName())
            .title(userProfileRequest.getTitle())
            .givenName(userProfileRequest.getGivenName())
            .familyName(userProfileRequest.getFamilyName())
            .email(userProfileRequest.getEmail())
            .phoneNumber(userProfileRequest.getPhone())
            .build();

        return userService.updateUser(id, userUpdate)
            .map(user -> {
                log.debug("Updated user [username: {}, id: {}]", user.getUsername(), user.getId());
                return marshal(user);
            })
            .map(user -> Response.ok(user).build())
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @PUT
    @Path("/password")
    public Response updatePassword(@Context SecurityContext ctx,
                                   PasswordUpdateRequest request) {
        UUID id = getUserId(ctx);
        log.info("Update user password [id: {}]", id);

        char[] currentPassword = request.getOldPassword().toCharArray();
        char[] newPassword = request.getNewPassword().toCharArray();
        userService.updatePassword(id, currentPassword, newPassword)
            .orElseThrow(() -> new NotAuthorizedException("username/password"));

        return Response.noContent().build();
    }

    private UUID getUserId(SecurityContext context) {
        return UUID.fromString(context.getUserPrincipal().getName());
    }

    private UserProfileResponse marshal(User user) {
        return new UserProfileResponse()
            .username(user.getUsername())
            .preferredName(user.getPreferredName())
            .title(user.getTitle())
            .givenName(user.getGivenName())
            .familyName(user.getFamilyName())
            .email(user.getEmail())
            .phone(user.getPhoneNumber())
            .dateCreated(user.getDateCreated())
            .dateOnboarded(user.getDateOnboarded());
    }
}
