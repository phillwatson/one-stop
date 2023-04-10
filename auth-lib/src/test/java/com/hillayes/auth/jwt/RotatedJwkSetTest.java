package com.hillayes.auth.jwt;

import org.junit.jupiter.api.Test;

import java.security.PrivateKey;

import static org.junit.jupiter.api.Assertions.*;

public class RotatedJwkSetTest {
    @Test
    public synchronized void testRotation() throws InterruptedException {
        RotatedJwkSet fixture = new RotatedJwkSet();
        fixture.jwkSetSize = 2;
        fixture.rotationInterval = 5;
        fixture.init();

        try {
            PrivateKey key1 = fixture.getCurrentPrivateKey();
            assertSame(key1, fixture.getCurrentPrivateKey());

            wait(6 * 1000);
            assertNotSame(key1, fixture.getCurrentPrivateKey());

            System.out.println(fixture.toJson());
        } finally {
            fixture.destroy();
        }
    }
}
