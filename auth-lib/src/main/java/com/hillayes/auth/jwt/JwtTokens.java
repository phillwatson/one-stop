package com.hillayes.auth.jwt;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Cookie;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class JwtTokens {
    @Inject
    JWTParser jwtParser;

    public Optional<JsonWebToken> getToken(String cookieName, Map<String, Cookie> cookies) {
        Cookie cookie = cookies.get(cookieName);
        if (cookie == null) {
            log.info("Cookie not found [name: {}]", cookieName);
            return Optional.empty();
        }

        try {
            return Optional.of(jwtParser.parse(cookie.getValue()));
        } catch (ParseException e) {
            log.warn("Failed to parse JWT", e);
            return Optional.empty();
        }
    }
}
