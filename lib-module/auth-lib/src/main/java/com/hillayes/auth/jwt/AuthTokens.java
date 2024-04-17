package com.hillayes.auth.jwt;

import com.hillayes.auth.xsrf.XsrfTokens;
import com.hillayes.commons.net.Gateway;
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

    // the path on which access-token cookies are submitted
    @ConfigProperty(name = "one-stop.auth.access-token.path", defaultValue = "/api")
    String accessCookiePath;

    @ConfigProperty(name = "one-stop.auth.access-token.expires-in")
    Duration accessDuration;

    @ConfigProperty(name = "one-stop.auth.refresh-token.cookie")
    String refreshCookieName;

    // the path on which refresh-token cookies are submitted
    @ConfigProperty(name = "one-stop.auth.refresh-token.path", defaultValue = "/api/v1/auth/refresh")
    String refreshCookiePath;

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

    @Inject
    Gateway gateway;

    @PostConstruct
    void init() {
        // audiences config prop is a comma-delimited list - we need a Set
        audiences = Arrays.stream(audiencesList.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
    }

    /**
     * Generates a signed JWT for the given principal, with the given expiry duration.
     * This is used when a user registers for an account with their email address.
     * The token (a one-time-token) will be emailed to them, and they will use it to
     * complete their registration.
     *
     * @param principalName the principal name to be used to identify the registration.
     * @param expiresIn the time-to-live of the token.
     * @return the signed JWT with the given principal and expiry duration.
     */
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
            .path(accessCookiePath)
            .version(NewCookie.DEFAULT_VERSION)
            .maxAge((int)accessDuration.toSeconds())
            .expiry(Date.from(Instant.now().plus(accessDuration)))
            .secure(gateway.isSecure())
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.STRICT)
            .build();

        NewCookie refreshTokenCookie = new NewCookie.Builder(refreshCookieName)
            .value(refreshToken)
            .path(refreshCookiePath)
            .version(NewCookie.DEFAULT_VERSION)
            .maxAge((int)refreshDuration.toSeconds())
            .expiry(Date.from(Instant.now().plus(refreshDuration)))
            .secure(gateway.isSecure())
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.STRICT)
            .build();

        // the value of this cookie must be read by the client-side script and
        // returned in the request header identified by the configuration property
        // "one-stop.auth.xsrf.header"
        NewCookie xsrfTokenCookie = new NewCookie.Builder(xsrfCookieName)
            .value(xsrfToken)
            .path("/")
            .version(NewCookie.DEFAULT_VERSION)
            .maxAge((int)refreshDuration.toSeconds())
            .expiry(Date.from(Instant.now().plus(refreshDuration)))
            .secure(gateway.isSecure())
            .httpOnly(false) // script must have access
            .sameSite(NewCookie.SameSite.STRICT)
            .build();

        return new NewCookie[]{accessTokenCookie, refreshTokenCookie, xsrfTokenCookie};
    }

    /**
     * Set auth cookies in the given response.
     *
     * @param responseBuilder   the response builder to which the cookies are to be added.
     * @param userPrincipalName the user principal name to be used in the JWT.
     * @param groups            the groups/roles to be used in the JWT.
     * @return the given response builder with the cookies added.
     */
    public Response authResponse(Response.ResponseBuilder responseBuilder,
                                 Object userPrincipalName, Set<String> groups) {
        for (NewCookie cookie : buildCookies(userPrincipalName, groups)) {
            responseBuilder.header("Set-Cookie", cookie);
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
        Date expires = Date.from(Instant.now());
        NewCookie accessToken = new NewCookie.Builder(accessCookieName)
            .path(accessCookiePath)
            .version(NewCookie.DEFAULT_VERSION)
            .maxAge(0)
            .expiry(expires)
            .secure(false)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.STRICT)
            .build();

        NewCookie refreshToken = new NewCookie.Builder(refreshCookieName)
            .path(refreshCookiePath)
            .version(NewCookie.DEFAULT_VERSION)
            .maxAge(0)
            .expiry(expires)
            .secure(false)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.STRICT)
            .build();

        return responseBuilder.cookie(accessToken, refreshToken).build();
    }
}
