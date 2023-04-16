package com.hillayes.user.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.PageLinks;
import com.hillayes.onestop.api.PaginatedUsers;
import com.hillayes.onestop.api.UserResponse;
import com.hillayes.onestop.api.UserUpdateRequest;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.UUID;

@Path("/api/v1/users")
@RolesAllowed({"admin"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserAdminResource {
    private static final String PAGE_LINK = "%s?page=%d&page-size=%d";

    private final UserService userService;

    @GET
    public Response listUsers(@Context UriInfo uriInfo,
                              @QueryParam("page") @DefaultValue("0") int page,
                              @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("Listing users [page: {}, page-size: {}]", page, pageSize);

        Page<User> usersPage = userService.listUsers(page, pageSize);
        PaginatedUsers response = new PaginatedUsers()
            .page(page)
            .pageSize(pageSize)
            .count(usersPage.getNumberOfElements())
            .total(usersPage.getTotalElements())
            .items(usersPage.getContent().stream().map(this::marshal).toList())
            .links(new PageLinks()
                .first(String.format(PAGE_LINK, uriInfo.getPath(), 0, pageSize))
                .last(String.format(PAGE_LINK, uriInfo.getPath(), usersPage.getTotalPages() - 1, pageSize))
            );

        if (page > 0) {
            response.getLinks().previous(String.format(PAGE_LINK, uriInfo.getPath(), page - 1, pageSize));
        }
        if (page < usersPage.getTotalPages() - 1) {
            response.getLinks().next(String.format(PAGE_LINK, uriInfo.getPath(), page + 1, pageSize));
        }

        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") UUID id) {
        log.info("Getting user [id: {}]", id);

        return userService.getUser(id)
            .map(user -> {
                log.debug("Found user [username: {}, id: {}]", user.getUsername(), user.getId());
                return marshal(user);
            })
            .map(user -> Response.ok(user).build())
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @POST
    public Response createUser(@Context UriInfo uriInfo,
                               UserUpdateRequest userRequest) {
        log.info("Creating user [username: {}]", userRequest.getUsername());
        User user = userService.createUser(userRequest.getUsername(), "password".toCharArray(), marshal(userRequest));

        log.debug("Created user [username: {}, id: {}]", user.getUsername(), user.getId());
        return Response
            .created(URI.create(uriInfo.getPath() + "/" + user.getId()))
            .entity(marshal(user))
            .build();
    }

    @PUT
    @Path("/{id}/onboard")
    public Response onboardUser(@PathParam("id") UUID id) {
        log.info("Onboarding user [id: {}]", id);
        return userService.onboardUser(id)
            .map(user -> {
                log.debug("Onboarded user [username: {}, id: {}]", user.getUsername(), user.getId());
                return Response.noContent().build();
            })
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @PUT
    @Path("/{id}")
    public Response updateUser(@PathParam("id") UUID id,
                               UserUpdateRequest userUpdateRequest) {
        log.info("Update user [id: {}]", id);
        return userService.updateUser(id, marshal(userUpdateRequest))
            .map(user -> {
                log.debug("Updated user [username: {}, id: {}]", user.getUsername(), user.getId());
                return marshal(user);
            })
            .map(user -> Response.noContent().build())
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") UUID id) {
        log.info("Delete user [id: {}]", id);
        return userService.deleteUser(id)
            .map(user -> {
                log.debug("Deleted user [username: {}, id: {}]", user.getUsername(), user.getId());
                return Response.noContent().build();
            })
            .orElseThrow(() -> new NotFoundException("user", id));
    }

    private User marshal(UserUpdateRequest userRequest) {
        return User.builder()
            .username(userRequest.getUsername())
            .preferredName(userRequest.getPreferredName())
            .title(userRequest.getTitle())
            .givenName(userRequest.getGivenName())
            .familyName(userRequest.getFamilyName())
            .email(userRequest.getEmail())
            .phoneNumber(userRequest.getPhone())
            .build();
    }

    private UserResponse marshal(User user) {
        return new UserResponse()
            .id(user.getId())
            .username(user.getUsername())
            .preferredName(user.getPreferredName())
            .title(user.getTitle())
            .givenName(user.getGivenName())
            .familyName(user.getFamilyName())
            .email(user.getEmail())
            .phone(user.getPhoneNumber())
            .dateCreated(user.getDateCreated())
            .dateOnboarded(user.getDateOnboarded())
            .dateBlocked(user.getDateBlocked())
            .roles(user.getRoles().stream().toList());
    }
}
