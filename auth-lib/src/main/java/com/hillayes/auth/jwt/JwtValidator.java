package com.hillayes.auth.jwt;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

public class JwtValidator {
    private final JwtConsumer jwtConsumer;

    public JwtValidator(String location, String issuer, String audience) {
        HttpsJwks keySet = new HttpsJwks(location);

        JwtConsumerBuilder builder = new JwtConsumerBuilder()
            .setRequireExpirationTime()
            .setAllowedClockSkewInSeconds(30)
            .setRequireSubject()
            .setVerificationKeyResolver(new HttpsJwksVerificationKeyResolver(keySet))
            .setJwsAlgorithmConstraints(
                // only allow the expected signature algorithm(s)
                AlgorithmConstraints.ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256
            );

        if (issuer != null) builder.setExpectedIssuer(issuer); // whom the JWT needs to have been issued by
        if (audience != null) builder.setExpectedAudience(audience); // to whom the JWT is intended for

        jwtConsumer = builder
            .build(); // create the JwtConsumer instance
    }

    public JwtClaims verify(String jwt) throws InvalidJwtException {
        //  Validate the JWT and process it to the Claims
        return jwtConsumer.processToClaims(jwt);
    }
}
