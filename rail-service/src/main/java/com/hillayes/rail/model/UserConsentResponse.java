package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hillayes.rail.domain.ConsentStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserConsentResponse {
    private UUID id;

    private String institutionId;

    private String institutionName;

    /**
     * Indicates the position in the flow to obtain consent from the user.
     */
    private ConsentStatus status;

    private List<AccountDetail> accounts;

    private Instant dateGiven;

    /**
     * The date-time on which the agreement expires.
     */
    private Instant agreementExpires;

    /**
     * The agreed number of past days for which transaction data can be obtained.
     */
    private int maxHistory;
}
