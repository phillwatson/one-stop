package com.hillayes.nordigen.model;

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
    /**
     * an Institution ID for this EUA
     */
    @JsonProperty("institution_id")
    private String institutionId;

    /**
     * Maximum number of days of transaction data to retrieve.
     */
    @JsonProperty("max_historical_days")
    private Integer maxHistoricalDays;

    /**
     * Number of days from acceptance that the access can be used.
     */
    @JsonProperty("access_valid_for_days")
    private Integer accessValidForDays;

    /**
     * Level of information to access (by default all).
     * Array containing one or several values of ['balances', 'details', 'transactions']
     */
    @JsonProperty("access_scope")
    private List<String> accessScope;

    /**
     * if this agreement can be extended. Supported by GB banks only.
     */
    @JsonProperty("reconfirmation")
    public boolean reconfirmation;
}
