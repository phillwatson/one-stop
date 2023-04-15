package com.hillayes.user.repository;

import com.hillayes.user.domain.OidcIdentity;
import com.hillayes.user.domain.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

import static com.hillayes.user.utils.TestData.mockUser;
import static com.hillayes.user.utils.TestData.mockUsers;
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

        List<User> users = userRepository.findByEmail(user.getEmail());
        assertEquals(1, users.size());
        assertEquals(2, users.get(0).getOidcIdentities().size());

        assertTrue(userRepository.findByIssuerAndSubject(googleId.getIssuer(), googleId.getSubject()).isPresent());
        assertFalse(userRepository.findByIssuerAndSubject(googleId.getIssuer(), UUID.randomUUID().toString()).isPresent());
        assertFalse(userRepository.findByIssuerAndSubject(UUID.randomUUID().toString(), googleId.getSubject()).isPresent());

        userRepository.delete(user);
        assertTrue(userRepository.findByEmail(user.getEmail()).isEmpty());
    }

    @Test
    public void testFindPaged() {
        List<User> users = mockUsers(20);
        userRepository.saveAll(users);

        int pageSize = 5;
        PageRequest pageRequest = PageRequest.of(1, pageSize, Sort.by("username").ascending());
        Page<User> result = userRepository.findByDateDeletedIsNull(pageRequest);

        assertEquals(21, result.getTotalElements());
        assertEquals(5, result.getTotalPages());
        assertEquals(pageSize, result.getContent().size());
        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
    }
}
