package com.hillayes.auth.jwt;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Cookie;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class JwtTokens {
    @Inject
    JWTParser jwtParser;

    /**
     * Locates the named cookie from the given Map and attempts to parse and verify its
     * content. If the named token cannot be found, or if it is not valid, the result
     * will be empty.
     *
     * @param cookieName the name of the cookie to be parsed and verified.
     * @param cookies the collection of cookies (typically from a client request).
     * @return the parsed JWT, or empty if not found or not valid.
     */
    public Optional<JsonWebToken> getToken(String cookieName, Map<String, Cookie> cookies) {
        Cookie cookie = cookies.get(cookieName);
        if (cookie == null) {
            log.info("Cookie not found [name: {}]", cookieName);
            return Optional.empty();
        }

        try {
            return Optional.of(parseAndVerify(cookie.getValue()));
        } catch (ParseException e) {
            log.warn("Failed to parse JWT", e);
            return Optional.empty();
        }
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
    public JsonWebToken parseAndVerify(String signedJwt) throws ParseException {
        return jwtParser.parse(signedJwt);
    }
}
