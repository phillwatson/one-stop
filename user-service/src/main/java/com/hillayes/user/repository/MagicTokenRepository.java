package com.hillayes.user.repository;

import com.hillayes.user.domain.MagicToken;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link MagicToken} records.
 */
@Singleton
public interface MagicTokenRepository extends JpaRepository<MagicToken, UUID> {
    /**
     * Returns the record holding the given token.
     *
     * @param token the token to search for.
     * @return the record found with the given token, if any.
     */
    public Optional<MagicToken> findByToken(String token);

    /**
     * Returns the record holding the given email.
     *
     * @param email the email to search for.
     * @return the record found with the given email, if any.
     */
    public Optional<MagicToken> findByEmail(String email);

    /**
     * Deletes all records with an expiry date before the given date.
     *
     * @param datetime the date before which all records should be deleted.
     */
    public long deleteByExpiresBefore(Instant datetime);
}
