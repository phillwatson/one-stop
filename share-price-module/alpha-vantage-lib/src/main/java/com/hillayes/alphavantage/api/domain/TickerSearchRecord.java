package com.hillayes.alphavantage.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(onlyExplicitlyIncluded = true)
public class TickerSearchRecord {
    @JsonProperty("1. symbol")
    @ToString.Include
    public String symbol;

    @JsonProperty("2. name")
    @ToString.Include
    public String name;

    @JsonProperty("3. type")
    public String type;

    @JsonProperty("4. region")
    public String region;

    @JsonProperty("5. marketOpen")
    public String marketOpen;

    @JsonProperty("6. marketClose")
    public String marketClose;

    @JsonProperty("7. timezone")
    public String timezone;

    @JsonProperty("8. currency")
    @ToString.Include
    public String currency;

    @JsonProperty("9. matchScore")
    public Double matchScore;
}
