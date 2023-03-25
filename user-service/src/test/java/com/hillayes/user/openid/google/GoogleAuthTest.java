package com.hillayes.user.openid.google;

import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.openid.OpenIdConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;

@QuarkusTest
public class GoogleAuthTest {
    @Inject
    @Named("googleConfig")
    OpenIdConfiguration.AuthConfig config;

    @Inject
    @Named("googleValidator")
    JwtValidator jwtValidator;

    @Inject
    GoogleAuth googleAuth;

    @Test
    public void test() throws InvalidJwtException {
        System.out.println(config.clientSecret());
        System.out.println(jwtValidator);

        googleAuth.exchangeAuthToken("test");
    }
}
