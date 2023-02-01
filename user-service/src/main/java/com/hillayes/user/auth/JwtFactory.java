package com.hillayes.user.auth;

import com.hillayes.user.domain.User;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.ApplicationScoped;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

@ApplicationScoped
public class JwtFactory {
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    private volatile KeyPair keyPair = null;

    private KeyPair getKeyPair() throws NoSuchAlgorithmException {
        if (keyPair == null) {
            synchronized (this) {
                if (keyPair == null) {
                    keyPair = KeyUtils.generateKeyPair(2048);
                }
            }
        }
        return keyPair;
    }

    public String createJwt(User user, long expiresInMins) throws NoSuchAlgorithmException {
        KeyPair keyPair = getKeyPair();

        String result = Jwt.claims()
            .issuer(issuer)
            .subject(user.getId().toString())
            .upn(user.getUsername())
            .groups("user")
            .expiresIn(expiresInMins)
            .sign(keyPair.getPrivate());
        return result;
    }

    public JsonWebToken parseJwt(String jwtString) throws NoSuchAlgorithmException, ParseException {
        KeyPair keyPair = getKeyPair();

        JWTParser parser = new DefaultJWTParser();
        return parser.verify(jwtString, keyPair.getPublic());
    }
}
