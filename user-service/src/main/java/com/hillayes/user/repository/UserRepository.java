package com.hillayes.user.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.user.domain.User;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository extends RepositoryBase<User, UUID> {
    public Page<User> findAll(Sort sort, int pageNumber, int pageSize) {
        return findByPage(findAll(sort), pageNumber, pageSize);
    }

    /**
     * Returns the user with the given username. Usernames are unique.
     *
     * @param username the username to search for.
     * @return the user record found with the given username, if any.
     */
    public Optional<User> findByUsername(String username) {
        return find("username", username)
            .firstResultOptional();
    }

    /**
     * Returns the users holding the given email address.
     *
     * @param email the email address to be searched. The value should be lower-case.
     * @return the user holding the given email address.
     */
    public Optional<User> findByEmail(String email) {
        return find("email", email)
            .firstResultOptional();
    }

    /**
     * Returns the user with a link to the given Open-ID Connect identity.
     *
     * @param issuer the issuer of the Open-ID Connect identity.
     * @param subject the subject of the Open-ID Connect identity.
     * @return the user with a link to the given Open-ID Connect identity.
     */
    public Optional<User> findByIssuerAndSubject(String issuer, String subject) {
        return find("SELECT DISTINCT u FROM User u " +
                "JOIN u.oidcIdentities oidc " +
                "WHERE oidc.issuer = :issuer AND oidc.subject = :subject",
            Parameters.with("issuer", issuer).and("subject", subject))
            //.project(User.class)
            .firstResultOptional();
    }
}
