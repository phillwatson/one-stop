package com.hillayes.auth.jwt;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtSignatureException;
import io.smallrye.jwt.util.KeyUtils;

import java.io.IOException;
import java.security.*;
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
 * After creating the JWT, sign it using the Elliptic Curve Digital Signature Algorithm (ECDSA) with the P-256 curve and the SHA-256 hash algorithm.
 */
public class ClientSecretGenerator {
    // aka - bundle ID
    private static final String CLIENT_ID = "velopaymentsHibanaPayQA";
    private static final String TEAM_ID = "S5474NHS5U";
    private static final String KEY_ID = "69V4Q9N572";

    private static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.ES256;

    public PrivateKey readPrivateKey(String file,
                                     String algorithm) throws GeneralSecurityException, IOException {
        return readPrivateKey(file, SignatureAlgorithm.fromAlgorithm(algorithm));
    }

    public PrivateKey readPrivateKey(String file,
                                     SignatureAlgorithm algorithm) throws GeneralSecurityException, IOException {
        return KeyUtils.readPrivateKey(file, algorithm);
    }

    public String createSignedToken(PrivateKey privateKey,
                                    String kid,
                                    String issuer,
                                    String subject,
                                    String audience,
                                    Duration duration,
                                    String algorithm) throws JwtSignatureException {
        return createSignedToken(privateKey, kid, issuer, subject, audience, duration,
            SignatureAlgorithm.fromAlgorithm(algorithm));
    }

    public String createSignedToken(PrivateKey privateKey,
                                    String kid,
                                    String issuer,
                                    String subject,
                                    String audience,
                                    Duration duration,
                                    SignatureAlgorithm algorithm) throws JwtSignatureException {
        // hint: to see public key from apple-id.p8
        // openssl pkey -pubout -in apple-id.p8

        return Jwt.claims()
            .issuer(issuer)
            .subject(subject)
            .audience(audience)
            .issuedAt(System.currentTimeMillis() / 1000L)
            .expiresIn(duration)
            .jws()
            .header("typ", "JWT")
            .keyId(kid)
            .algorithm(algorithm)
            .sign(privateKey);
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        ClientSecretGenerator instance = new ClientSecretGenerator();

        PrivateKey privateKey = instance.readPrivateKey("apple-id.p8", ALGORITHM);

        String signedToken = instance.createSignedToken(privateKey,
            KEY_ID, TEAM_ID, CLIENT_ID, "https://appleid.apple.com",
            Duration.ofHours(15), ALGORITHM);
        System.out.println(signedToken);

    }
}
