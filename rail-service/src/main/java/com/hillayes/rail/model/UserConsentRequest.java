package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserConsentRequest {
    /**
     * The rail ID for the institution to which the consent request refers.
     */
    private String institutionId;

    /**
     * The URI to which the user should be redirected after they have given or
     * denied consent.
     */
    private String callbackUri;
}
