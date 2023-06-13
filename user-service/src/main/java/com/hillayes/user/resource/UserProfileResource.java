package com.hillayes.user.resource;

import com.hillayes.commons.Strings;
import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.PasswordUpdateRequest;
import com.hillayes.onestop.api.UserProfileRequest;
import com.hillayes.onestop.api.UserProfileResponse;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.UUID;

@Path("/api/v1/profiles")
@RolesAllowed({"user", "admin"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserProfileResource {
    private final UserService userService;

    @GET
    public Response getProfile(@Context SecurityContext ctx) {
        UUID id = ResourceUtils.getUserId(ctx);
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
    public Response updateProfile(@Context SecurityContext ctx,
                                  UserProfileRequest request) {
        UUID id = ResourceUtils.getUserId(ctx);
        log.info("Update user profile [id: {}]", id);

        User userUpdate = User.builder()
            .username(request.getUsername())
            .preferredName(request.getPreferredName())
            .title(request.getTitle())
            .givenName(request.getGivenName())
            .familyName(request.getFamilyName())
            .email(request.getEmail())
            .phoneNumber(request.getPhone())
            .build();

        return userService.updateUser(id, userUpdate)
            .map(user -> {
                log.debug("Updated user profile [id: {}, username: {}]", user.getId(), user.getUsername());
                return marshal(user);
            })
            .map(user -> Response.ok(user).build())
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @PUT
    @Path("/password")
    public Response changePassword(@Context SecurityContext ctx,
                                   PasswordUpdateRequest request) {
        UUID id = ResourceUtils.getUserId(ctx);
        log.info("Update user password [id: {}]", id);

        if (Strings.isBlank(request.getOldPassword())) {
            throw new MissingParameterException("oldPassword");
        }

        if (Strings.isBlank(request.getNewPassword())) {
            throw new MissingParameterException("newPassword");
        }

        char[] currentPassword = request.getOldPassword().toCharArray();
        char[] newPassword = request.getNewPassword().toCharArray();
        return userService.updatePassword(id, currentPassword, newPassword)
            .map(user -> Response.noContent().build())
            .orElseThrow(() -> new NotAuthorizedException("username/password"));
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
