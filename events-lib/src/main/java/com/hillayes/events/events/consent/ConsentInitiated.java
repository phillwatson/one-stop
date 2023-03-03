package com.hillayes.events.events.consent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Notifies listeners of the initiation of the process to obtain a user's consent
 * to access their bank account details.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentInitiated {
    /**
     * The ID of the user from whom consent is being obtained.
     */
    @NotNull
    private UUID userId;

    /**
     * The date-time on which the consent process was initiated.
     */
    @NotNull
    private Instant dateInitiated;

    /**
     * The ID of the consent record held user-consent table.
     */
    @NotNull
    private UUID consentId;

    /**
     * The ID of the institution for which consent is being obtained. This record is
     * held in the rail providing the open-banking service.
     */
    @NotNull
    private String institutionId;

    /**
     * The ID of the agreement between the user for whom consent is being obtained. This
     * record is held in the rail providing the open-banking service.
     */
    @NotNull
    private String agreementId;

    /**
     * The date-time at which the agreement will expire.
     */
    @NotNull
    private Instant agreementExpires;

    /**
     * The ID of the requisition to grant access to the user's bank account. This
     * record is held in the rail providing the open-banking service.
     */
    @NotNull
    private String requisitionId;
}
