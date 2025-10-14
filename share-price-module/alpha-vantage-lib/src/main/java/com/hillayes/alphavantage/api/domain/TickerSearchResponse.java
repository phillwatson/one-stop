package com.hillayes.alphavantage.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class TickerSearchResponse {
    @JsonProperty("bestMatches")
    public List<TickerSearchRecord> series;
}
