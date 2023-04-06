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
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
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

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "userconsent_id", nullable = false)
    private UUID userConsentId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "institution_id", nullable = false)
    private String institutionId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "rail_account_id", nullable = false)
    private String railAccountId;

    @Column(name = "iban", nullable = true)
    private String iban;

    @Column(name = "account_name", nullable = true)
    private String accountName;

    @Column(name = "account_type", nullable = true)
    private String accountType;

    @Column(name = "owner_name", nullable = true)
    private String ownerName;

    @Column(name = "currency_code", nullable = true)
    private String currencyCode;

    /**
     * The date-time that the account details were last polled. The account details
     * won't be polled again until a configured grace period has elapsed.
     *
     * @See ServiceConfiguration.accountPollingInterval()
     */
    @Column(name = "date_last_polled", nullable = true)
    private Instant dateLastPolled;
}
