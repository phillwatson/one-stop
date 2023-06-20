package com.hillayes.user.resource;

import com.hillayes.auth.jwt.AuthTokens;
import com.hillayes.onestop.api.UserCompleteRequest;
import com.hillayes.onestop.api.UserRegisterRequest;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import io.smallrye.jwt.auth.principal.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.jwt.JsonWebToken;

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
    public Response registerUser(@Context UriInfo uriInfo,
                                 UserRegisterRequest request) {
        log.info("Registering user [email: {}]", request.getEmail());
        userService.registerUser(request.getEmail(), uriInfo.getBaseUriBuilder());

        // don't give away whether the email is registered or not
        return Response.accepted().build();
    }

    @POST
    @Path("/complete")
    @PermitAll
    public Response onboardUser(@Context SecurityContext ctx,
                                UserCompleteRequest request) {
        log.info("Completing user registration [token: {}]", request.getToken());

        try {
            JsonWebToken jwt = authTokens.getToken(request.getToken());
            User newUser = User.builder()
                .username(request.getUsername())
                .givenName(request.getGivenName())
                .email(jwt.getName())
                .build();

            newUser = userService.completeOnboarding(newUser, request.getPassword().toCharArray());
            return authTokens.authResponse(Response.noContent(), newUser.getId(), newUser.getRoles());
        } catch (ParseException e) {
            log.error("Onboarding JWT is invalid [token: {}]", request.getToken());
            throw new NotAuthorizedException("token");
        }
    }
}
