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
public class RequisitionRequest {
    @JsonProperty("redirect")
    private String redirect;

    @JsonProperty("institution_id")
    private String institutionId;

    @JsonProperty("agreement")
    private String agreement;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("user_language")
    private String userLanguage;

    @JsonProperty("ssn")
    private String ssn;

    @JsonProperty("account_selection")
    private Boolean accountSelection;

    @JsonProperty("redirect_immediate")
    private Boolean redirectImmediate;
}
