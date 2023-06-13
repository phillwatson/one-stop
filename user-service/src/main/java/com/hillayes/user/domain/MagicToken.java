package com.hillayes.user.domain;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

/**
 * MagicTokens are user during the user onboarding process. The token is sent
 * (as a url link) to the email given by the user wishing to onboard. By opening
 * the link the user acknowledges the email address is valid; a {@link User}
 * record is created, and the token deleted.
 * The token has a short lifespan, after which it is no longer valid.
 */
@Entity(name = "magic_token")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class MagicToken {
    @Id
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    /**
     * The token identifying the user onboarding request.
     */
    @Column(nullable = false)
    private String token;

    /**
     * The email address the token was sent to.
     */
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private String email;

    /**
     * The time at which the token expires.
     */
    @Column(nullable = false)
    private Instant expires;
}
