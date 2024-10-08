package com.hillayes.alphavantage.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DayRecord {
    @JsonProperty("1. open")
    public Double open;
    @JsonProperty("2. high")
    public Double high;
    @JsonProperty("3. low")
    public Double low;
    @JsonProperty("4. close")
    public Double close;
    @JsonProperty("5. volume")
    public Integer volume;
}
