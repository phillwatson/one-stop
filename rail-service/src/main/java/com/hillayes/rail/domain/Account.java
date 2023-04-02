package com.hillayes.rail.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.Instant;
import java.util.UUID;

/**
 * Records the identifiers of the bank accountDetails to which a user has given
 * consent.
 */
@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @Column(name = "userconsent_id", nullable = false)
    private UUID userConsentId;

    @Column(name = "rails_account_id", nullable = false)
    private String railsAccountId;
}
