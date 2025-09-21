package com.hillayes.events.events.consent;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Notifies listeners of the registration of a new bank account.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class AccountRegistered {
    /**
     * The ID of the user from whom consent is being obtained.
     */
    private UUID userId;

    /**
     * The date-time on which the bank account was registered.
     */
    private Instant dateRegistered;

    /**
     * The ID of the consent record held in the user-consent table.
     */
    private UUID consentId;

    /**
     * The ID of the institution for which consent is being obtained. This record is
     * held in the rail providing the open-banking service.
     */
    private String institutionId;

    /**
     * The name of the institution for which consent is being obtained.
     */
    private String institutionName;

    /**
     * The ID of the account record held in the account table.
     */
    private UUID accountId;
}
