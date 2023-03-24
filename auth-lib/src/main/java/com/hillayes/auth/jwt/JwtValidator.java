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
    private final HttpsJwks keySet;
    private final JwtConsumer jwtConsumer;

    public JwtValidator(String location, String issuer, String audience) {
        keySet = new HttpsJwks(location);
        JwtConsumerBuilder builder = new JwtConsumerBuilder()
            .setRequireExpirationTime() // the JWT must have an expiration time
            .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
            .setRequireSubject() // the JWT must have a subject claim
            .setVerificationKeyResolver(new HttpsJwksVerificationKeyResolver(keySet)) // verify the signature with the public key
            .setJwsAlgorithmConstraints(
                // only allow the expected signature algorithm(s) in the given context
                AlgorithmConstraints.ConstraintType.PERMIT,
                AlgorithmIdentifiers.RSA_USING_SHA256 // which is only RS256 here
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
