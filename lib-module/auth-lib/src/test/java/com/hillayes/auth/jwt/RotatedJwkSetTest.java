package com.hillayes.auth.jwt;

import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class RotatedJwkSetTest {
    @Test
    public synchronized void testRotation_RSA() throws InterruptedException {
        RotatedJwkSet fixture = new RotatedJwkSet();
        fixture.jwkSetSize = 2;
        fixture.rotationInterval = Duration.ofSeconds(2);
        fixture.init();

        try {
            PrivateKey key1 = fixture.getCurrentPrivateKey();
            assertSame(key1, fixture.getCurrentPrivateKey());
            assertEquals("RSA", key1.getAlgorithm());

            wait(3 * 1000);

            PrivateKey key2 = fixture.getCurrentPrivateKey();
            assertNotSame(key1, key2);
            assertEquals("RSA", key2.getAlgorithm());
        } finally {
            fixture.destroy();
        }
    }
    @Test
    public synchronized void testRotation_EC() throws InterruptedException {
        RotatedJwkSet fixture = new RotatedJwkSet();
        fixture.algorithm = RotatedJwkSet.EC_ALGORITHM;
        fixture.jwkSetSize = 2;
        fixture.rotationInterval = Duration.ofSeconds(2);
        fixture.init();

        try {
            PrivateKey key1 = fixture.getCurrentPrivateKey();
            assertSame(key1, fixture.getCurrentPrivateKey());
            assertEquals("EC", key1.getAlgorithm());

            wait(3 * 1000);

            PrivateKey key2 = fixture.getCurrentPrivateKey();
            assertNotSame(key1, key2);
            assertEquals("EC", key2.getAlgorithm());
        } finally {
            fixture.destroy();
        }
    }
}
