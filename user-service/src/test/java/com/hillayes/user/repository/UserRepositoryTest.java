package com.hillayes.user.repository;

import com.hillayes.user.domain.OidcIdentity;
import com.hillayes.user.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class UserRepositoryTest {
    @Inject
    UserRepository userRepository;

    @Test
    public void testInsert() {
        User user = User.builder()
            .username("tester")
            .givenName("test")
            .familyName("user")
            .email("email")
            .phoneNumber("123456")
            .passwordHash(UUID.randomUUID().toString())
            .roles(Set.of("tester"))
            .build();
        OidcIdentity googleId = user.addOidcIdentity("google", UUID.randomUUID().toString());
        user.addOidcIdentity("apple", UUID.randomUUID().toString());

        user = userRepository.save(user);

        List<User> users = userRepository.findByEmail(user.getEmail());
        assertEquals(1, users.size());
        assertEquals(2, users.get(0).getOidcIdentities().size());

        assertTrue(userRepository.findByIssuerAndSubject(googleId.getIssuer(), googleId.getSubject()).isPresent());
        assertFalse(userRepository.findByIssuerAndSubject(googleId.getIssuer(), UUID.randomUUID().toString()).isPresent());
        assertFalse(userRepository.findByIssuerAndSubject(UUID.randomUUID().toString(), googleId.getSubject()).isPresent());

        userRepository.delete(user);
        assertTrue(userRepository.findByEmail(user.getEmail()).isEmpty());
    }
}
