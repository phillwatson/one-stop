package com.hillayes.rail.api.domain;

import lombok.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Agreement {
    /**
     * The rail ID for the agreement.
     */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String id;

    /**
     * The date-time on which the agreement was created, but not yet acknowledged. 
     */
    @Builder.Default
    @ToString.Include
    private Instant dateCreated = Instant.now();

    /**
     * The date-time on which the agreement was acknowledged.
     */
    @ToString.Include
    private Instant dateGiven;

    /**
     * The date-time on which the agreement expires.
     */
    private Instant dateExpires;

    /**
     * The rail ID for the institution to which the agreement refers.
     */
    @ToString.Include
    private String institutionId;

    private List<String> accountIds;

    /**
     * The agreed number of past days for which transaction data can be obtained.
     */
    private int maxHistory;

    /**
     * Indicates the position in the flow to obtain agreement from the user.
     */
    @ToString.Include
    private AgreementStatus status;

    /**
     * The URL to which the user is redirected in order to obtain their authorisation
     * to access their account details.
     */
    private URI agreementLink;

    /**
     * If agreement is denied, this records the error code returned by the rail.
     */
    private String errorCode;

    /**
     * If agreement is denied, this records the detail of the error returned by the rail.
     */
    private String errorDetail;
}
