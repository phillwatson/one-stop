package com.hillayes.auth.jwt;

import com.hillayes.executors.concurrent.ExecutorConfiguration;
import com.hillayes.executors.concurrent.ExecutorFactory;
import com.hillayes.executors.concurrent.ExecutorType;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;

import javax.security.auth.DestroyFailedException;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the generation and rotation of a collection of Json Web Keys by periodically
 * generating a new key-pair, discarding the oldest key-pair when the max-keys has been
 * reached.
 * <p>
 * The rotation-interval and set-size are tied together to determine the max lifetime
 * for a signature. When a key-pair is discarded any data signed by the private key of
 * the pair can no longer be verified; effectively invalidating that signature.
 * <p>
 * For a JWT with an expiration time claim ("exp"), this max lifetime is important.
 * If the key-pair used to sign and verify the token are discarded before the expiration
 * time, the JWT will be considered invalid. Therefore, the expiration time should be
 * set to a value less than the <code>rotation-interval * set-size</code>.
 */
@ApplicationScoped
@Slf4j
public class RotatedJwkSet {
    /**
     * Determines the frequency at which new key-pairs are generated and added
     * to the set.
     */
    @ConfigProperty(name = "one-stop.auth.jwk.rotation-interval")
    Duration rotationInterval;

    /**
     * Determines the maximum number of key-pairs held in the set. When this
     * number is exceeded the oldest key-pairs are discarded.
     */
    @ConfigProperty(name = "one-stop.auth.jwk.set-size", defaultValue = "2")
    int jwkSetSize;

    /**
     * The lifo stack of key-pairs. Access to this is synchronized.
     */
    private final Deque<RsaJsonWebKey> stack = new LinkedList<>();

    private ScheduledExecutorService executor;

    /**
     * Constructs a JWK Set that does not exceed the given maximum number of keys, and
     * are rotated at the given interval (in seconds).
     */
    @PostConstruct
    public void init() {
        rotateKeys();
        executor =
            (ScheduledExecutorService) ExecutorFactory.newExecutor(ExecutorConfiguration.builder()
                .executorType(ExecutorType.SCHEDULED)
                .name("JWK-Rotation")
                .numberOfThreads(1)
                .build());

        executor.scheduleAtFixedRate(this::rotateKeys,
            rotationInterval.toSeconds(), rotationInterval.toSeconds(), TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        log.debug("Shutting down keyset");
        executor.shutdown();
        synchronized (stack) {
            stack.forEach(this::disposeOf);
            stack.clear();
        }
    }

    /**
     * Called periodically (based on rotation-interval) to generate a new key-pair.
     * If, after generating a key-pair, the number of key-pairs exceeds the set-size,
     * the oldest key-pairs will be discarded until the set-size is correct.
     */
    public void rotateKeys() {
        log.debug("Rotating JWK-Set");
        synchronized (stack) {
            try {
                String kid = String.valueOf(System.currentTimeMillis());
                stack.addLast(newJWK(kid));
                while (stack.size() > jwkSetSize) {
                    disposeOf(stack.pop());
                }
            } catch (JoseException e) {
                log.error("Failed to rotate JWK-Set", e);
            }
        }
    }

    /**
     * Generates a new public/private key-pair; assigning the given key-id (kid).
     * The kid will be used by clients to identify the key-pair when verifying a
     * signature.
     *
     * @param kid the unique identifier for the key-pair.
     * @return the generated key-pair.
     * @throws JoseException if the key cannot be created.
     */
    private RsaJsonWebKey newJWK(String kid) throws JoseException {
        RsaJsonWebKey result = RsaJwkGenerator.generateJwk(2048);
        result.setKeyId(kid);

        return result;
    }

    /**
     * Disposes the given public/private key-pair, ensuring that it can no longer
     * be used; even if another handle is held on that key-pair.
     *
     * @param jwk the key-pair to be disposed.
     */
    private void disposeOf(RsaJsonWebKey jwk) {
        try {
            jwk.getPrivateKey().destroy();
        } catch (DestroyFailedException ignore) {
        }
    }

    /**
     * Returns the most recently generated key-pair.
     */
    private RsaJsonWebKey getCurrentWebKey() {
        synchronized (stack) {
            return stack.getLast();
        }
    }

    /**
     * Returns the most recently generated private key.
     */
    public PrivateKey getCurrentPrivateKey() {
        return getCurrentWebKey().getRsaPrivateKey();
    }

    /**
     * Returns the current set of public keys as a Json Web-Key Set. This is used
     * to publish those keys to external clients.
     */
    public String toJson() {
        synchronized (stack) {
            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(stack.stream().toList());
            return jsonWebKeySet.toJson();
        }
    }

    /**
     * Signs the given JWT claims with the most recent private key. Also sets the "kid"
     * claim with the ID of the most recent public key, so that the key can later be
     * identified when verifying the signature.
     *
     * @param jwtClaimsBuilder the JWT claims to be signed.
     * @return the signed JWT token.
     */
    public String signClaims(JwtClaimsBuilder jwtClaimsBuilder) {
        RsaJsonWebKey webKey = getCurrentWebKey();
        return jwtClaimsBuilder
            .jws()
            .keyId(webKey.getKeyId())
            .sign(webKey.getPrivateKey());
    }
}
