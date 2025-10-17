package com.hillayes.alphavantage.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Overview {
    @JsonProperty("Symbol")
    public String symbol;

    @JsonProperty("Name")
    public String name;

    @JsonProperty("Currency")
    public String currency;
}
