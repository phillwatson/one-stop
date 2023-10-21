package com.hillayes.auth.jwt;

import com.hillayes.auth.xsrf.XsrfTokens;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class AuthTokens {
    @ConfigProperty(name = "one-stop.auth.xsrf.cookie", defaultValue = "XSRF-TOKEN")
    String xsrfCookieName;

    @ConfigProperty(name = "one-stop.auth.access-token.cookie")
    String accessCookieName;

    @ConfigProperty(name = "one-stop.auth.access-token.expires-in")
    Duration accessDuration;

    @ConfigProperty(name = "one-stop.auth.refresh-token.cookie")
    String refreshCookieName;

    @ConfigProperty(name = "one-stop.auth.refresh-token.expires-in")
    Duration refreshDuration;

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

    public String generateToken(String principalName, Duration expiresIn) {
        return jwkSet.signClaims(Jwt
            .issuer(issuer)
            .audience(audiences)
            .upn(principalName)
            .expiresIn(expiresIn));
    }

    /**
     * Constructs the auth cookies for the identified user, with the given groups (user roles).
     * The cookies include a signed access-token, signed refresh-token and a cross-site
     * request forgery (XSRF) token.
     *
     * @param userPrincipalName the identity of the authenticated user, by which all services
     * can identify the user (typically the user record primary key).
     * @param groups the user groups (roles) to which authenticated user belongs.
     * @return the array of cookies containing auth tokens.
     */
    private NewCookie[] buildCookies(Object userPrincipalName, Set<String> groups) {
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

        NewCookie accessTokenCookie = new NewCookie.Builder(accessCookieName)
            .value(accessToken)
            .path("/api")
            .version(NewCookie.DEFAULT_VERSION)
            .maxAge((int)accessDuration.toSeconds())
            .expiry(Date.from(Instant.now().plus(accessDuration)))
            .secure(false)
            .httpOnly(true)
            .build();

        NewCookie refreshTokenCookie = new NewCookie.Builder(refreshCookieName)
            .value(refreshToken)
            .path("/api/v1/auth/refresh")
            .version(NewCookie.DEFAULT_VERSION)
            .maxAge((int)refreshDuration.toSeconds())
            .expiry(Date.from(Instant.now().plus(refreshDuration)))
            .secure(false)
            .httpOnly(true)
            .build();

        NewCookie xsrfTokenCookie = new NewCookie.Builder(xsrfCookieName)
            .value(xsrfToken)
            .path("/")
            .version(NewCookie.DEFAULT_VERSION)
            .maxAge((int)refreshDuration.toSeconds())
            .expiry(Date.from(Instant.now().plus(refreshDuration)))
            .secure(false)
            .httpOnly(false) // script must have access
            .build();

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
                                 Object userPrincipalName, Set<String> groups) {
        for (NewCookie cookie : buildCookies(userPrincipalName, groups)) {
            responseBuilder.header("Set-Cookie", cookie + ";SameSite=Strict");
        }
        return responseBuilder.build();
    }

    /**
     * Reads any refresh cookie from the given collection and returns its parsed
     * and validated JWT content.
     *
     * @param cookies the collection of cookies in which the refresh cookie is found.
     * @return the parsed and verified content of the refresh token. Empty if not found.
     */
    public Optional<JsonWebToken> getRefreshToken(Map<String, Cookie> cookies) {
        return jwtTokens.getToken(refreshCookieName, cookies);
    }

    /**
     * Parse the given signed JWT, and verifies its signature using the public key
     * found in the location provided by the configuration property
     * <code>mp.jwt.verify.publickey.location</code>
     *
     * @param signedJwt the signed JWT.
     * @return the parsed and verified JWT.
     * @throws ParseException if the JWT is invalid.
     */
    public JsonWebToken getToken(String signedJwt) throws ParseException {
        return jwtTokens.parseAndVerify(signedJwt);
    }

    /**
     * Deletes the auth cookies by overwriting any existing cookies with new cookies
     * of the same name but with their expiry date-time to now.
     *
     * @param responseBuilder   the response builder to which the cookies are to be added.
     * @return the given response builder with the cookies added.
     */
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