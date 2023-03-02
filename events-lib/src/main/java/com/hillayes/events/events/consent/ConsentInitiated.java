package com.hillayes.events.events.consent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentInitiated {
    @NotNull
    private UUID consentId;

    @NotNull
    private Instant dateInitiated;

    @NotNull
    private UUID userId;

    @NotNull
    private String institutionId;

    @NotNull
    private String agreementId;

    @NotNull
    private Instant agreementExpires;

    @NotNull
    private String requisitionId;
}
