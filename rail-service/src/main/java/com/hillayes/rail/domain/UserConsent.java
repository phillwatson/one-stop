package com.hillayes.rail.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a user's consent to provide access to their accountDetails held at a
 * given bank.
 */
@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class UserConsent {
    @Id
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    @Builder.Default
    @ToString.Include
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @ToString.Include
    @Column(name = "date_given", nullable = true)
    @Setter
    private Instant dateGiven;

    @Column(name = "date_denied", nullable = true)
    @Setter
    private Instant dateDenied;

    @Column(name = "date_cancelled", nullable = true)
    @Setter
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

    /**
     * The date-time on which the agreement expires.
     */
    @Column(name = "agreement_expires", nullable = false)
    private Instant agreementExpires;

    /**
     * The agreed number of past days for which transaction data can be obtained.
     */
    @Column(name = "max_history", nullable = false)
    private int maxHistory;

    /**
     * The rail ID for the requisition for access to which the consent refers.
     */
    @ToString.Include
    @Setter
    @Column(name = "requisition_id", nullable = true)
    private String requisitionId;

    /**
     * The URL to which the client will be redirected after consent request is completed.
     */
    @Setter
    @Column(name = "callback_uri", nullable = true)
    private String callbackUri;

    /**
     * Indicates the position in the flow to obtain consent from the user.
     */
    @ToString.Include
    @Setter
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConsentStatus status;

    /**
     * If consent is denied, this records the error code returned by the rail.
     */
    @Setter
    @Column(name="error_code", nullable = true)
    private String errorCode;

    /**
     * If consent is denied, this records the detail of the error returned by the rail.
     */
    @Setter
    @Column(name="error_detail", nullable = true)
    private String errorDetail;
}
