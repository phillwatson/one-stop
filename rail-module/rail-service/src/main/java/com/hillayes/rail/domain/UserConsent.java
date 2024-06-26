package com.hillayes.rail.domain;

import com.hillayes.rail.api.domain.RailProvider;
import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a user's consent to provide access to their accountDetails held at a
 * given bank.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class UserConsent {
    @Id
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    private RailProvider provider;

    @Builder.Default
    @ToString.Include
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @ToString.Include
    @Column(name = "date_given")
    private Instant dateGiven;

    @Column(name = "date_denied")
    private Instant dateDenied;

    @Column(name = "date_cancelled")
    private Instant dateCancelled;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The rail ID for the institution to which the consent refers.
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "institution_id", nullable = false)
    private String institutionId;

    /**
     * The rail ID for the agreement to which the consent refers.
     */
    @ToString.Include
    @Column(name = "agreement_id", nullable = false)
    private String agreementId;

    @Column(name = "reference", nullable = false)
    private String reference;

    /**
     * The date-time on which the agreement expires.
     */
    @Column(name = "agreement_expires")
    private Instant agreementExpires;

    /**
     * The agreed number of past days for which transaction data can be obtained.
     */
    @Column(name = "max_history", nullable = false)
    private int maxHistory;

    /**
     * The URL to which the client will be redirected after consent request is completed.
     */
    @Column(name = "callback_uri")
    private String callbackUri;

    /**
     * Indicates the position in the flow to obtain consent from the user.
     */
    @ToString.Include
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConsentStatus status;

    /**
     * If consent is denied, this records the error code returned by the rail.
     */
    @Column(name="error_code")
    private String errorCode;

    /**
     * If consent is denied, this records the detail of the error returned by the rail.
     */
    @Column(name="error_detail")
    private String errorDetail;
}
