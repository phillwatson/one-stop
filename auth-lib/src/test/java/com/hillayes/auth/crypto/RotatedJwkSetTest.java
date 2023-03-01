package com.hillayes.auth.crypto;

import com.hillayes.auth.crypto.RotatedJwkSet;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;

import static org.junit.jupiter.api.Assertions.*;

public class RotatedJwkSetTest {
    @Test
    public synchronized void testRotation() throws InterruptedException {
        RotatedJwkSet fixture = new RotatedJwkSet(2, 5);
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
