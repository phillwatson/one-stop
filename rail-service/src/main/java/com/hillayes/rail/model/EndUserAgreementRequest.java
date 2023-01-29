package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EndUserAgreementRequest {
    @JsonProperty("max_historical_days")
    private Integer maxHistoricalDays;

    @JsonProperty("access_valid_for_days")
    private Integer accessValidForDays;

    @JsonProperty("access_scope")
    private List<String> accessScope;

    @JsonProperty("institution_id")
    private String institutionId;
}
