package com.hillayes.user.auth;

import com.hillayes.user.domain.User;
import io.restassured.internal.util.IOUtils;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTParser;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JwtFactoryTest {
    JwtFactory jwtFactory = new JwtFactory();

    @BeforeAll
    public static void initKey() {
//        System.setProperty("smallrye.jwt.sign.key.location", "resource/auth/rsaPrivateKey.pem");
    }

    @Test
    public void test() throws Exception {
        User user = User.builder()
            .id(UUID.randomUUID())
            .username("snoopy")
            .email("snoopy@peanuts.com")
            .givenName("Snoopy")
            .familyName("Brown")
            .phoneNumber("01 2843939")
            .dateCreated(Instant.parse("2018-11-29T18:35:24.00Z"))
            .dateOnboarded(Instant.parse("2018-11-30T12:21:34.00Z"))
            .build();

        String jwt = jwtFactory.createJwt(user, 30 * 60);

        JsonWebToken jsonWebToken = jwtFactory.parseJwt(jwt);
        assertEquals(user.getId().toString(), jsonWebToken.getSubject());
        assertEquals(user.getUsername(), jsonWebToken.getName());
        assertTrue(jsonWebToken.getGroups().contains("user"));
    }

    @Test
    public void testVerify() throws Exception {
        String jwtString = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2hpbGxheWVzLmNvbS9vbmUtc3RvcCIsInVwbiI6Impkb2VAcXVhcmt1cy5pbyIsImdyb3VwcyI6WyJ1c2VyIl0sImJpcnRoZGF0ZSI6IjIwMDEtMDctMTMiLCJpYXQiOjE2NzUwOTc3NTksImV4cCI6MTY3NTA5ODA1OSwianRpIjoiNzY0MzA4Y2QtNGFmZS00OGMwLTgxN2YtYWRkNzlhZDRiOWE3In0.G76iXANIRD9ryfRCit54oabAF4IOoFTgNizVGBm03IfmhITPmIrgN-qQXNiaWKzQI03lVhbV_AnjMFvZkE556TPcg-2fg9ETFsYrti_4HkAQNYkhnbxEY3oAi1P8aGCyb1zlIXVDFJXx4O8Sm9RmJK7k3PXuMpg4PsS2wZ7Rznj1Rz__PntlBOs7FXaur9lG0wWSIKMpGCw0YiyABCgHMH7hfbB5tLNScl6WCdKIxTwAZIsKmHQ3ne3zZ40Vd30e0rFtYeUBxnEI834ovfdZXy_EuAsd2quTmj0Npb5QUx6yMr2_xrsyHI4OQqF0oCgV60bnJgm59AXqD4looHr7UA";

        PublicKey key = getPublicKey("/auth/publicKey.pem");
        JWTParser parser = new DefaultJWTParser();
        JsonWebToken jsonWebToken = parser.verify(jwtString, key);

        assertEquals("", jsonWebToken.getSubject());
        assertEquals("", jsonWebToken.getName());
        assertTrue(jsonWebToken.getGroups().contains("user"));
    }

    private PublicKey getPublicKey(String filename) throws Exception {
        byte[] keyBytes = IOUtils.toByteArray(getClass().getResourceAsStream(filename));

        keyBytes = Base64.getDecoder().decode(new String(keyBytes)
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replaceAll(System.lineSeparator(), "")
            .replace("-----END PUBLIC KEY-----", "")
            .trim());

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
        return publicKey;
    }
}
