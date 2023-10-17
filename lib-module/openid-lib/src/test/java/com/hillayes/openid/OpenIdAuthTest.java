package com.hillayes.openid;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OpenIdAuthTest {
    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdAuth googleAuth;

    @Inject
    @Any
    Instance<OpenIdAuth> instances;

    @Test
    public void testQualifiedInjections() {
        assertEquals("OpenIdAuth[https://accounts.google.com/.well-known/openid-configuration]", googleAuth.toString());
    }

    @Test
    public void testAllInstances() {
        assertFalse(instances.stream().toList().isEmpty());

        assertTrue(instances.stream()
            .anyMatch(instance -> instance.isFor(AuthProvider.GOOGLE)));
    }
}
