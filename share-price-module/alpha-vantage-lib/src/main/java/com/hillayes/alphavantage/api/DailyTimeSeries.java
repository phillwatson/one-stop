package com.hillayes.alphavantage.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyTimeSeries {
    @JsonProperty("Time Series (Daily)")
    Map<LocalDate, DayRecord> series;
}
