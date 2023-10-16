package com.hillayes.events.events.consent;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Notifies listeners of the expiration of a user's consent to access their bank account details.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class ConsentExpired {
    /**
     * The ID of the user from whom consent is being obtained.
     */
    private UUID userId;

    /**
     * The date-time on which the consent expired.
     */
    private Instant dateExpired;

    /**
     * The ID of the consent record held user-consent table.
     */
    private UUID consentId;

    /**
     * The ID of the institution for which consent has expired. This record is
     * held in the rail providing the open-banking service.
     */
    private String institutionId;

    /**
     * The name of the institution for which consent has expired.
     */
    private String institutionName;

    /**
     * The ID of the agreement between the user for whom consent is being obtained. This
     * record is held in the rail providing the open-banking service.
     */
    private String agreementId;

    /**
     * The date-time at which the agreement will expire.
     */
    private Instant agreementExpires;

    /**
     * The ID of the requisition to grant access to the user's bank account. This
     * record is held in the rail providing the open-banking service.
     */
    private String requisitionId;
}
