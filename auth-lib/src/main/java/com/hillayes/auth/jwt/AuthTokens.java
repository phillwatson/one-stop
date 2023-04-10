package com.hillayes.auth.jwt;

import com.hillayes.auth.xsrf.XsrfTokens;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class AuthTokens {
    @ConfigProperty(name = "one-stop.auth.xsrf.cookie-name", defaultValue = "XSRF-TOKEN")
    String xsrfCookieName;

    @ConfigProperty(name = "one-stop.auth.access-token.cookie")
    String accessCookieName;

    @ConfigProperty(name = "one-stop.auth.access-token.duration-secs")
    int accessDuration;

    @ConfigProperty(name = "one-stop.auth.refresh-token.cookie")
    String refreshCookieName;

    @ConfigProperty(name = "one-stop.auth.refresh-token.duration-secs")
    int refreshDuration;

    @ConfigProperty(name = "one-stop.auth.access-token.issuer")
    String issuer;

    @ConfigProperty(name = "one-stop.auth.access-token.audiences")
    String audiencesList;

    private Set<String> audiences;

    @Inject
    JwtTokens jwtTokens;

    /**
     * The JWK Set containing the private key used to sign the auth JWTs.
     */
    @Inject
    RotatedJwkSet jwkSet;

    /**
     * The XSRF validator used to generate XSRF tokens.
     */
    @Inject
    XsrfTokens xsrfTokens;

    @PostConstruct
    void init() {
        // audiences config prop is a comma-delimited list - we need a Set
        audiences = Arrays.stream(audiencesList.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
    }

    private NewCookie[] buildCookies(UUID userPrincipalName, Set<String> groups) {
        log.info("Building auth tokens [issuer: {}, userId: {}]", issuer, userPrincipalName);

        String xsrfToken = xsrfTokens.generateToken();

        String accessToken = jwkSet.signClaims(Jwt
            .issuer(issuer)
            .audience(audiences)
            .upn(userPrincipalName.toString()) // this will be the Principal of the security context
            .expiresIn(accessDuration)
            .groups(groups)
            .claim("xsrf", xsrfToken));

        String refreshToken = jwkSet.signClaims(Jwt
            .issuer(issuer)
            .audience(audiences)
            .upn(userPrincipalName.toString()) // this will be the Principal of the security context
            .expiresIn(refreshDuration)
            .claim("xsrf", xsrfToken));

        NewCookie accessTokenCookie = new NewCookie(accessCookieName, accessToken,
            "/api", null, NewCookie.DEFAULT_VERSION, null,
            accessDuration, Date.from(Instant.now().plusSeconds(accessDuration)),
            false, true);

        NewCookie refreshTokenCookie = new NewCookie(refreshCookieName, refreshToken,
            "/api/v1/auth/refresh", null, NewCookie.DEFAULT_VERSION, null,
            refreshDuration, Date.from(Instant.now().plusSeconds(refreshDuration)),
            false, true);

        NewCookie xsrfTokenCookie = new NewCookie(xsrfCookieName, xsrfToken,
            "/", null, NewCookie.DEFAULT_VERSION, null,
            refreshDuration, Date.from(Instant.now().plusSeconds(refreshDuration)),
            false, false);

        return new NewCookie[]{accessTokenCookie, refreshTokenCookie, xsrfTokenCookie};
    }

    /**
     * Set auth cookies in the given response, adding the SameSite=Strict attribute to
     * each cookie.
     *
     * @param responseBuilder   the response builder to which the cookies are to be added.
     * @param userPrincipalName the user principal name to be used in the JWT.
     * @param groups            the groups/roles to be used in the JWT.
     * @return the given response builder with the cookies added.
     */
    public Response authResponse(Response.ResponseBuilder responseBuilder,
                                 UUID userPrincipalName, Set<String> groups) {
        for (NewCookie cookie : buildCookies(userPrincipalName, groups)) {
            responseBuilder.header("Set-Cookie", cookie + ";SameSite=Strict");
        }
        return responseBuilder.build();
    }

    public Optional<JsonWebToken> getRefreshToken(Map<String, Cookie> cookies) throws ParseException {
        return jwtTokens.getToken(refreshCookieName, cookies);
    }

    public Response deleteCookies(Response.ResponseBuilder responseBuilder) {
        NewCookie accessToken = new NewCookie(accessCookieName, null,
            "/api", null, NewCookie.DEFAULT_VERSION, null,
            0, Date.from(Instant.now()), false, true);

        NewCookie refreshToken = new NewCookie(refreshCookieName, null,
            "/api/v1/auth/refresh", null, NewCookie.DEFAULT_VERSION, null,
            0, Date.from(Instant.now()), false, true);

        return responseBuilder.cookie(accessToken, refreshToken).build();
    }
}
