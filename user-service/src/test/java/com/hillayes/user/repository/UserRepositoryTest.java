package com.hillayes.user.repository;

import com.hillayes.user.domain.OidcIdentity;
import com.hillayes.user.domain.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

import static com.hillayes.user.utils.TestData.mockUser;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
public class UserRepositoryTest {
    @Inject
    UserRepository userRepository;

    @Test
    public void testInsert() {
        User user = mockUser();
        OidcIdentity googleId = user.addOidcIdentity("google", UUID.randomUUID().toString());
        user.addOidcIdentity("apple", UUID.randomUUID().toString());

        user = userRepository.save(user);

        Optional<User> users = userRepository.findByEmail(user.getEmail());
        assertTrue(users.isPresent());
        assertEquals(2, users.get().getOidcIdentities().size());

        assertTrue(userRepository.findByIssuerAndSubject(googleId.getIssuer(), googleId.getSubject()).isPresent());
        assertFalse(userRepository.findByIssuerAndSubject(googleId.getIssuer(), UUID.randomUUID().toString()).isPresent());
        assertFalse(userRepository.findByIssuerAndSubject(UUID.randomUUID().toString(), googleId.getSubject()).isPresent());

        userRepository.delete(user);
        assertTrue(userRepository.findByEmail(user.getEmail()).isEmpty());
    }
}
