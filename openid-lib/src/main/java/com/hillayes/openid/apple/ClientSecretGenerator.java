package com.hillayes.openid.apple;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtSignatureException;
import io.smallrye.jwt.util.KeyUtils;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.time.Duration;

/**
 * JSON Web Token (JWT) is an open-standard (RFC 7519) that defines a way to transmit information
 * securely. Sign in with Apple requires JWTs to authorize each validation request. Create the
 * token, then sign it with the private key you downloaded from Apple Developer.
 * <p>
 * To generate a signed JWT:
 * <ol>
 * <li>Create the JWT header.</li>
 * <li>Create the JWT payload.</li>
 * <li>Sign the JWT.</li>
 * </ol>
 * To create a JWT, use the following fields and values in the JWT header:
 * <dl>
 * <dt>alg</dt>
 * <dd>The algorithm used to sign the token. For Sign in with Apple, use ES256.</dd>
 * <dt>kid</dt>
 * <dd>A 10-character key identifier generated for the Sign in with Apple private key associated
 * with your developer account. The JWT payload contains information specific to the Sign in with
 * Apple REST API and the client app, such as issuer, subject, and expiration time. Use the
 * following claims in the payload:</dd>
 * <dt>iss</dt>
 * <dd>The issuer registered claim identifies the principal that issued the client secret. Since
 * the client secret belongs to your developer team, use your 10-character Team ID associated
 * with your developer account.</dd>
 * <dt>iat</dt>
 * <dd>The issued at registered claim indicates the time at which you generated the client secret,
 * in terms of the number of seconds since Epoch, in UTC.</dd>
 * <dt>exp</dt>
 * <dd>The expiration time registered claim identifies the time on or after which the client
 * secret expires. The value must not be greater than 15777000 (6 months in seconds) from the
 * Current UNIX Time on the server.</dd>
 * <dt>aud</dt>
 * <dd>The audience registered claim identifies the intended recipient of the client secret.
 * Since the client secret is sent to the validation server, use https://appleid.apple.com.</dd>
 * <dt>sub</dt>
 * <dd>The subject registered claim identifies the principal that is the subject of the client
 * secret. Since this client secret is meant for your application, use the same value as client_id.
 * The value is case-sensitive.
 * </dd>
 * </dl>
 * After creating the JWT, sign it using the Elliptic Curve Digital Signature Algorithm (ECDSA)
 * with the P-256 curve and the SHA-256 hash algorithm.
 */
@ApplicationScoped
public class ClientSecretGenerator {
    /**
     * Reads a private key from the PEM (Privacy Enhanced Mail) file whose path is given.
     *
     * @param file the path of the PEM file - specified as per [Class.getResourceAsStream].
     * @param algorithm the key signature algorithm.
     * @return the resolved Private Key.
     */
    public PrivateKey readPrivateKey(String file,
                                     SignatureAlgorithm algorithm) throws GeneralSecurityException, IOException {
        return KeyUtils.readPrivateKey(file, algorithm);
    }

    /**
     * Reads a private key from the given PEM (Privacy Enhanced Mail) payload.
     *
     * @param pem the path of the PEM payload.
     * @param algorithm the key signature algorithm.
     * @return the resolved Private Key.
     */
    public PrivateKey decodePrivateKey(String pem,
                                       SignatureAlgorithm algorithm) throws GeneralSecurityException {
        return KeyUtils.decodePrivateKey(pem, algorithm);
    }

    /**
     * Creates a Json Web Token with the given claims and signs it with the given
     * Private Key.
     *
     * The *kid* parameter is allocated by Apple and will be placed in the generated
     * JWT's headers. The recipient of the JWT will then use that value to locate the
     * Public Key by which the JWT's signature can be verified.
     *
     * @param privateKey the Private Key with which to sign to the generated JWT.
     * @param kid the Key Identifier to be placed in the JWT headers.
     * @param teamId the JWT issuer claim value. (e.g. the Team ID)
     * @param clientId the JWT subject claim value. (e.g. the Client ID)
     * @param audience the JWT audience claim value. (e.g. https://appleid.apple.com)
     * @param duration the duration to be used for the JWT expiry claim value.
     * @param algorithm the algorithm to be used to sign the generated JWT.
     * @return a string holding the encoded signed JWT.
     */
    public String createClientSecret(PrivateKey privateKey,
                                    String kid,
                                    String teamId,
                                    String clientId,
                                    String audience,
                                    Duration duration,
                                    SignatureAlgorithm algorithm) throws JwtSignatureException {
        // hint: to see public key from apple-id.p8
        // openssl pkey -pubout -in apple-id.p8

        return Jwt.claims()
            .issuer(teamId)
            .subject(clientId)
            .audience(audience)
            .issuedAt(System.currentTimeMillis() / 1000L)
            .expiresIn(duration)
            .jws()
            .header("typ", "JWT")
            .keyId(kid)
            .algorithm(algorithm)
            .sign(privateKey);
    }
}
