package com.hillayes.auth.jwt;

import com.hillayes.executors.concurrent.ExecutorConfiguration;
import com.hillayes.executors.concurrent.ExecutorFactory;
import com.hillayes.executors.concurrent.ExecutorType;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
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
 */
@ApplicationScoped
@Slf4j
public class RotatedJwkSet {
    @ConfigProperty(name = "one-stop.auth.jwk.rotation-interval")
    Duration rotationInterval;

    @ConfigProperty(name = "one-stop.auth.jwk.set-size", defaultValue = "2")
    int jwkSetSize;

    /**
     * The lifo stack of key-pairs.
     */
    private final Deque<RsaJsonWebKey> stack = new LinkedList<>();

    /**
     * The ID assigned to the most recently generated key.
     */
    private long currentKid = 0;

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

    public void rotateKeys() {
        log.debug("Rotating JWK-Set");
        synchronized (stack) {
            try {
                currentKid = System.currentTimeMillis();
                stack.addLast(newJWK(String.valueOf(currentKid)));
                while (stack.size() > jwkSetSize) {
                    disposeOf(stack.pop());
                }
            } catch (JoseException e) {
                log.error("Failed to rotate JWK-Set", e);
            }
        }
    }

    public PrivateKey getCurrentPrivateKey() {
        synchronized (stack) {
            return stack.getLast().getRsaPrivateKey();
        }
    }

    public String toJson() {
        synchronized (stack) {
            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(stack.stream().toList());
            return jsonWebKeySet.toJson();
        }
    }

    private RsaJsonWebKey newJWK(String kid) throws JoseException {
        RsaJsonWebKey result = RsaJwkGenerator.generateJwk(2048);
        result.setKeyId(kid);

        return result;
    }

    private void disposeOf(RsaJsonWebKey jwk) {
        try {
            jwk.getPrivateKey().destroy();
        } catch (DestroyFailedException ignore) {
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
        synchronized (stack) {
            PrivateKey privateKey = getCurrentPrivateKey();

            return jwtClaimsBuilder
                .jws()
                .keyId(String.valueOf(currentKid))
                .sign(privateKey);
        }
    }
}
