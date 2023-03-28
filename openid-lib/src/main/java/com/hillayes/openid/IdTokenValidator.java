package com.hillayes.openid;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

/**
 * Maintains a cached list of the Json Web Keys found at the given URI,
 * periodically refreshing the cache. With those public keys it can
 * parse JWT tokens and verify the claims and signatures of those tokens.
 */
public class IdTokenValidator {
    private final JwtConsumer jwtConsumer;

    public IdTokenValidator(String location, String issuer, String audience) {
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

    /**
     * Parse and verify the claims of the given JWT (given as a raw string).
     *
     * @param jwt the Json web Token to be parsed and verified.
     * @return the claims found in the given JWT.
     * @throws InvalidJwtException if the token is invalid.
     */
    public JwtClaims verify(String jwt) throws InvalidJwtException {
        //  Validate the JWT and process it to the Claims
        return jwtConsumer.processToClaims(jwt);
    }
}
