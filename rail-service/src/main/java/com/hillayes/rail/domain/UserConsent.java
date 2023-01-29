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
    @Column(nullable = false)
    private UUID userId;

    @EqualsAndHashCode.Include
    @Column(name = "institution_id", nullable = false)
    private String institutionId;

    @Column(name = "agreement_id", nullable = false)
    private String agreementId;

    @Column(name = "agreement_expires", nullable = false)
    private Instant agreementExpires;

    @Column(name = "max_history", nullable = false)
    private int maxHistory;

    @Setter
    @Column(name = "requisition_id", nullable = true)
    private String requisitionId;

    @Setter
    @Column(nullable = false)
    private ConsentStatus status;
}
