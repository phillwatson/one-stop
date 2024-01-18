package com.hillayes.user.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.user.domain.User;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository extends RepositoryBase<User, UUID> {
    /**
     * Returns the user with the given username. Usernames are unique.
     *
     * @param username the username to search for.
     * @return the user record found with the given username, if any.
     */
    public Optional<User> findByUsername(String username) {
        return findFirst("username", username);
    }

    /**
     * Returns the users holding the given email address.
     *
     * @param email the email address to be searched. The value should be lower-case.
     * @return the user holding the given email address.
     */
    public Optional<User> findByEmail(String email) {
        return findFirst("email", email);
    }

    /**
     * Returns the user with a link to the given Open-ID Connect identity.
     *
     * @param issuer the issuer of the Open-ID Connect identity.
     * @param subject the subject of the Open-ID Connect identity.
     * @return the user with a link to the given Open-ID Connect identity.
     */
    public Optional<User> findByIssuerAndSubject(String issuer, String subject) {
        return findFirst("SELECT DISTINCT u FROM User u " +
                "JOIN u.oidcIdentities oidc " +
                "WHERE oidc.issuer = :issuer AND oidc.subject = :subject",
            Map.of("issuer", issuer, "subject", subject));
    }
}
