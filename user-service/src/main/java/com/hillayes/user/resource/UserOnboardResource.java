package com.hillayes.user.resource;

import com.hillayes.auth.jwt.AuthTokens;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.UserCompleteRequest;
import com.hillayes.onestop.api.UserRegisterRequest;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.UUID;

@Path("/api/v1/users/onboard")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserOnboardResource {
    private final UserService userService;
    private final AuthTokens authTokens;

    @POST
    @Path("/register")
    @PermitAll
    public Response registerUser(UserRegisterRequest request) {
        log.info("Registering user [email: {}]", request.getEmail());

        userService.registerUser(request.getEmail());
        return Response.noContent().build();
    }

    @GET
    @Path("/acknowledge/{token}")
    @PermitAll
    public Response acknowledgeUser(@Context UriInfo uriInfo,
                                    @PathParam("token") String token) {
        log.info("Acknowledging user [token: {}]", token);

        return userService.acknowledgeToken(token)
            .map(user -> {
                // redirect to the onboard page - with auth tokens in cookies
                URI redirect = uriInfo.getBaseUri().resolve("/onboard");
                return authTokens.authResponse(Response.temporaryRedirect(redirect), user.getId(), user.getRoles());
            })
            .orElseThrow(() -> new NotAuthorizedException("token"));
    }

    @POST
    @Path("/complete")
    @RolesAllowed("onboarding")
    public Response onboardUser(@Context SecurityContext ctx,
                                UserCompleteRequest request) {
        UUID id = ResourceUtils.getUserId(ctx);
        log.info("Completing user registration [userId: {}, username: {}]", id, request.getUsername());

        User userUpdate = User.builder()
            .username(request.getUsername())
            .preferredName(request.getPreferredName())
            .title(request.getTitle())
            .givenName(request.getGivenName())
            .familyName(request.getFamilyName())
            .email(request.getEmail())
            .phoneNumber(request.getPhone())
            .build();

        return userService.completeOnboarding(id, userUpdate, request.getPassword().toCharArray())
            .map(user -> authTokens.authResponse(Response.noContent(), user.getId(), user.getRoles()))
            .orElseThrow(() -> new NotFoundException("user", id));
    }
}
