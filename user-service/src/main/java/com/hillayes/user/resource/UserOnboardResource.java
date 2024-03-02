package com.hillayes.user.resource;

import com.hillayes.auth.audit.RequestHeaders;
import com.hillayes.auth.jwt.AuthTokens;
import com.hillayes.commons.Strings;
import com.hillayes.onestop.api.UserCompleteRequest;
import com.hillayes.onestop.api.UserRegisterRequest;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Path("/api/v1/users/onboard")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserOnboardResource {
    private final UserService userService;
    private final AuthTokens authTokens;
    private final RequestHeaders requestHeaders;

    @POST
    @Path("/register")
    @PermitAll
    public Response registerUser(@Valid UserRegisterRequest request) {
        log.info("Registering user [email: {}]", Strings.maskEmail(request.getEmail()));
        userService.registerUser(request.getEmail());

        // don't give away whether the email is registered or not
        return Response.accepted().build();
    }

    @POST
    @Path("/complete")
    @PermitAll
    public Response onboardUser(@Context UriInfo uriInfo,
                                @Valid UserCompleteRequest request) {
        log.info("Completing user registration [token: {}]", request.getToken());

        try {
            JsonWebToken jwt = authTokens.getToken(request.getToken());
            List<Locale> languages = requestHeaders.getAcceptableLanguages();

            User newUser = User.builder()
                .username(request.getUsername())
                .givenName(request.getGivenName())
                .email(jwt.getName())
                .locale(languages.isEmpty() ? null : languages.getFirst())
                .build();

            newUser = userService.completeOnboarding(newUser, request.getPassword().toCharArray());

            URI location = uriInfo.getBaseUriBuilder()
                .path(UserProfileResource.class)
                .path(newUser.getId().toString())
                .build();
            Response response = authTokens.authResponse(Response.created(location), newUser.getId(), newUser.getRoles());
            return response;
        } catch (ParseException e) {
            log.error("Onboarding JWT is invalid [token: {}]", request.getToken());
            throw new NotAuthorizedException("token");
        }
    }
}
