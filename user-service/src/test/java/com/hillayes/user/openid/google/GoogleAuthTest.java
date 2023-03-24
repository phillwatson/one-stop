package com.hillayes.user.openid.google;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class GoogleAuthTest {
    @Inject
    GoogleAuth googleAuth;

    @Test
    public void test() {
    }
}
