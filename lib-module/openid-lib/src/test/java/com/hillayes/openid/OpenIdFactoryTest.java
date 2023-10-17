package com.hillayes.openid;

import com.hillayes.openid.rest.OpenIdTokenApi;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OpenIdFactoryTest {
    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdConfiguration.AuthConfig googleConfig;

    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    IdTokenValidator googleValidator;

    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdTokenApi googleTokenApi;

    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    OpenIdConfiguration.AuthConfig appleConfig;

    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    IdTokenValidator appleValidator;

    @Inject
    @NamedAuthProvider(AuthProvider.APPLE)
    OpenIdTokenApi appleTokenApi;

    @Test
    public void testQualifiedInjections() {
        assertEquals("https://appleid.apple.com/.well-known/openid-configuration", appleConfig.configUri());
        assertEquals("https://accounts.google.com/.well-known/openid-configuration", googleConfig.configUri());

        assertNotNull(googleValidator);
        assertNotNull(appleValidator);
        assertNotSame(googleValidator, appleValidator);

        assertNotNull(googleTokenApi);
        assertNotNull(appleTokenApi);
        assertNotSame(googleTokenApi, appleTokenApi);
    }
}
