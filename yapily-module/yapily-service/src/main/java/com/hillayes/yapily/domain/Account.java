package com.hillayes.yapily.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "rail_id", nullable = false)
    @Enumerated(EnumType.STRING)
    private RailId railId;

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    /**
     * The date-time that the account details were last polled. The account details
     * won't be polled again until a configured grace period has elapsed.
     *
     * @See ServiceConfiguration.accountPollingInterval()
     */
    @Column(name = "date_last_polled", nullable = true)
    private Instant dateLastPolled;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

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
}
