package com.hillayes.user.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.*;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/v1/users")
@RolesAllowed({"admin"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserAdminResource {
    private final UserService userService;

    @GET
    public Response listUsers(@Context UriInfo uriInfo,
                              @QueryParam("page") @DefaultValue("0") int page,
                              @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("Listing users [page: {}, page-size: {}]", page, pageSize);

        Page<User> usersPage = userService.listUsers(page, pageSize);
        PaginatedUsers response = new PaginatedUsers()
            .page(usersPage.getNumber())
            .pageSize(usersPage.getSize())
            .count(usersPage.getNumberOfElements())
            .total(usersPage.getTotalElements())
            .items(usersPage.getContent().stream().map(this::marshal).toList())
            .links(ResourceUtils.buildPageLinks(uriInfo, usersPage));

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

    @PUT
    @Path("/{id}")
    public Response updateUser(@PathParam("id") UUID id,
                               UserUpdateRequest request) {
        log.info("Update user [id: {}]", id);
        return userService.updateUser(id, marshal(request))
            .map(user -> {
                log.debug("Updated user [username: {}, id: {}]", user.getUsername(), user.getId());
                return marshal(user);
            })
            .map(user -> Response.ok(user).build())
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

    private User marshal(UserUpdateRequest request) {
        Set<String> roles = request.getRoles().stream()
            .map(UserRole::getValue)
            .collect(Collectors.toSet());

        return User.builder()
            .username(request.getUsername())
            .preferredName(request.getPreferredName())
            .title(request.getTitle())
            .givenName(request.getGivenName())
            .familyName(request.getFamilyName())
            .email(request.getEmail())
            .phoneNumber(request.getPhone())
            .roles(roles)
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
