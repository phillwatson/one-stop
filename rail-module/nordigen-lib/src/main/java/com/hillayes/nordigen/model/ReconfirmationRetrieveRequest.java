package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReconfirmationRetrieveRequest {
    /**
     * Optional redirect URL for reconfirmation to override requisition's redirect.
     */
    @JsonProperty("redirect")
    private String redirect;
}
