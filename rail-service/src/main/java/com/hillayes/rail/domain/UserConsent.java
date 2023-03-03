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
 * Records a user's consent to provide access to their accounts held at a
 * given bank.
 */
@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserConsent {
    @Id
    @GeneratedValue(generator = "uuid2")
    private UUID id;

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @EqualsAndHashCode.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The rail ID for the institution to which the consent refers.
     */
    @EqualsAndHashCode.Include
    @Column(name = "institution_id", nullable = false)
    private String institutionId;

    /**
     * The rail ID for the agreement to which the consent refers.
     */
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
    @Setter
    @Column(name = "requisition_id", nullable = true)
    private String requisitionId;

    /**
     * Indicates the position in the flow to obtain consent from the user.
     */
    @Setter
    @Column(nullable = false)
    private ConsentStatus status;

    /**
     * If consent is denied, this records the error code returned by the rail.
     */
    @Setter
    @Column(nullable = true)
    private String errorCode;

    /**
     * If consent is denied, this records the detail of the error returned by the rail.
     */
    @Setter
    @Column(nullable = true)
    private String errorDetail;
}
