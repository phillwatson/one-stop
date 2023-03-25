package com.hillayes.user.openid.google;

import com.hillayes.user.openid.AuthProvider;
import com.hillayes.user.openid.NamedAuthProvider;
import com.hillayes.user.openid.OpenIdAuth;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class GoogleAuthTest {
    @Inject
    @NamedAuthProvider(AuthProvider.GOOGLE)
    OpenIdAuth googleAuth;

    @Test
    public void testInjection() {
        assertEquals("OpenIdAuth[https://accounts.google.com/.well-known/openid-configuration]", googleAuth.toString());
    }

}
