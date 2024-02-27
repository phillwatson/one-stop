package com.hillayes.user.repository;

import com.hillayes.openid.AuthProvider;
import com.hillayes.user.domain.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.hillayes.user.utils.TestData.mockUsers;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
public class UserRepositoryTest {
    @Inject
    UserRepository fixture;

    @Test
    public void testFindByIssuerAndSubject() {
        // given: some users in the database with some oidc identities
        Iterable<User> users = mockUsers(3).stream()
            .map(user -> {
                user.addOidcIdentity(AuthProvider.GOOGLE, "google", UUID.randomUUID().toString());
                user.addOidcIdentity(AuthProvider.APPLE, "apple", UUID.randomUUID().toString());
                return user;
            })
            .toList();
        users = fixture.saveAll(users);

        // and: the data is flushed and the cache is cleared
        fixture.flush();
        fixture.clearCache();

        users.forEach(user -> {
            user.getOidcIdentities().forEach(oidc -> {
                // when: we search for each user by their issuer and subject
                Optional<User> found = fixture.findByIssuerAndSubject(oidc.getIssuer(), oidc.getSubject());

                // then: we should find the user
                assertTrue(found.isPresent());
                assertEquals(user.getId(), found.get().getId());
            });
        });
    }

    @Test
    public void testFindByIssuerAndSubject_NotFound() {
        // given: some users in the database with some oidc identities
        fixture.saveAll(
            mockUsers(3).stream()
                .peek(user -> {
                    user.addOidcIdentity(AuthProvider.GOOGLE, "google", UUID.randomUUID().toString());
                    user.addOidcIdentity(AuthProvider.APPLE, "apple", UUID.randomUUID().toString());
                })
                .toList());

        // and: the data is flushed and the cache is cleared
        fixture.flush();
        fixture.clearCache();

        // when: we search for a user by unknown issue and subject
        Optional<User> found = fixture.findByIssuerAndSubject("google", UUID.randomUUID().toString());

        // then: we should NOT find the user
        assertTrue(found.isEmpty());
    }

    @Test
    public void testUpdateOidcIdentityLastUsed() {
        // given: some users in the database with some oidc identities
        Iterable<User> users = mockUsers(3).stream()
            .map(user -> {
                user.addOidcIdentity(AuthProvider.GOOGLE, "google", UUID.randomUUID().toString());
                user.addOidcIdentity(AuthProvider.APPLE, "apple", UUID.randomUUID().toString());
                return user;
            })
            .toList();
        users = fixture.saveAll(users);

        // and: the data is flushed and the cache is cleared
        fixture.flush();
        fixture.clearCache();

        users.forEach(user ->
            user.getOidcIdentities().forEach(oidc -> {
                // when: we search for each user by their issuer and subject
                Optional<User> found = fixture.findByIssuerAndSubject(oidc.getIssuer(), oidc.getSubject());

                // then: we should find the user
                assertTrue(found.isPresent());
                User userFound = found.get();
                assertEquals(user.getId(), userFound.getId());

                // and: we update the last used date of the oidc identity
                userFound.getOidcIdentity(oidc.getIssuer())
                    .ifPresent(oidcIdentity ->  oidcIdentity.setDateLastUsed(Instant.now()));

                // and: we can update the user
                fixture.saveAndFlush(userFound);
            })
        );
    }

    @Test
    public void testFindByEmail() {
        // given: some users in the database
        Iterable<User> users = fixture.saveAll(mockUsers(3));

        // and: the data is flushed and the cache is cleared
        fixture.flush();
        fixture.clearCache();

        users.forEach(user -> {
            // when: we search for a user with an existing email
            Optional<User> found = fixture.findByEmail(user.getEmail().toLowerCase());

            // then: we should find the user
            assertTrue(found.isPresent());
            assertEquals(user.getEmail().toLowerCase(), found.get().getEmail().toLowerCase());
            assertEquals(user.getId(), found.get().getId());
        });
    }

    @Test
    public void testFindByEmail_NotFound() {
        // given: some users in the database
        fixture.saveAll(mockUsers(3));

        // and: the data is flushed and the cache is cleared
        fixture.flush();
        fixture.clearCache();

        // when: we search for a user with a non-existent email
        Optional<User> found = fixture.findByEmail("mock-user@work.com");

        // then: we should not find any user
        assertFalse(found.isPresent());
    }

    @Test
    public void testFindByUsername() {
        // given: some users in the database
        Iterable<User> users = fixture.saveAll(mockUsers(3));

        // and: the data is flushed and the cache is cleared
        fixture.flush();
        fixture.clearCache();

        users.forEach(user -> {
            // when: we search for a user with an existing username
            Optional<User> found = fixture.findByUsername(user.getUsername());

            // then: we should find the user
            assertTrue(found.isPresent());
            assertEquals(user.getUsername(), found.get().getUsername());
            assertEquals(user.getId(), found.get().getId());
        });
    }

    @Test
    public void testFindByUsername_NotFound() {
        // given: some users in the database
        fixture.saveAll(mockUsers(3));

        // and: the data is flushed and the cache is cleared
        fixture.flush();
        fixture.clearCache();

        // when: we search for a user with a non-existent username
        Optional<User> found = fixture.findByUsername("mock-username");

        // then: we should not find any user
        assertFalse(found.isPresent());
    }
}
