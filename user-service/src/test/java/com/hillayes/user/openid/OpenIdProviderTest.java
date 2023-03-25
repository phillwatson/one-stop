package com.hillayes.user.openid;

import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.openid.google.GoogleAuth;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OpenIdProviderTest {
    @Inject
    @AuthProviderNamed(AuthProvider.GOOGLE)
    OpenIdConfiguration.AuthConfig googleConfig;

    @Inject
    @AuthProviderNamed(AuthProvider.APPLE)
    OpenIdConfiguration.AuthConfig appleConfig;

    @Inject
    @AuthProviderNamed(AuthProvider.GOOGLE)
    JwtValidator googleValidator;

    @Inject
    @AuthProviderNamed(AuthProvider.APPLE)
    JwtValidator appleValidator;

    @Inject
    GoogleAuth googleAuth;

    @Test
    public void testQualifiedInjections() {
        assertEquals("https://appleid.apple.com/.well-known/openid-configuration", appleConfig.configUri());
        assertEquals("https://accounts.google.com/.well-known/openid-configuration", googleConfig.configUri());
        assertNotNull(googleValidator);
        assertNotNull(appleValidator);
        assertNotSame(googleValidator, appleValidator);

        assertEquals("OpenIdAuth[https://accounts.google.com/.well-known/openid-configuration]", googleAuth.toString());
    }
}
