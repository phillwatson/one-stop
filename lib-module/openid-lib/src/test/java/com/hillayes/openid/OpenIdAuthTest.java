package com.hillayes.openid;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Arrays;

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
        assertEquals(AuthProvider.values().length, instances.stream().count());
        Arrays.stream(AuthProvider.values()).forEach(provider ->
            assertTrue(instances.stream()
                .anyMatch(instance -> instance.isFor(provider)))
        );
    }
}
