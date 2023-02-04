package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserConsentRequest {
    @JsonProperty("institution_id")
    private String institutionId;
}
