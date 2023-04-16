package com.hillayes.user.repository;

import com.hillayes.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

@Singleton
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Returns the user with the given username. Usernames are unique, even across
     * deleted users.
     *
     * @param username the username to search for.
     * @return the user record found with the given username, if any.
     */
    public Optional<User> findByUsername(String username);

    /**
     * Returns the users holding the given email address.
     *
     * @param email the email address to be searched.
     * @return the user holding the given email address.
     */
    public Optional<User> findByEmail(String email);

    /**
     * Returns the user with a link to the given Open-ID Connect identity.
     *
     * @param issuer the issuer of the Open-ID Connect identity.
     * @param subject the subject of the Open-ID Connect identity.
     * @return the user with a link to the given Open-ID Connect identity.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.oidcIdentities oidc WHERE oidc.issuer = ?1 AND oidc.subject = ?2")
    public Optional<User> findByIssuerAndSubject(String issuer, String subject);
}
