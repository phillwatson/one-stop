package com.hillayes.user.auth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class PasswordCryptoTest {
    @Test
    public void test() throws NoSuchAlgorithmException {
        PasswordCrypto fixture = new PasswordCrypto();
        String hash = fixture.getHash("password".toCharArray());
        System.out.println(hash);
    }
}
