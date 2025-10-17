package com.hillayes.alphavantage.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeeklyTimeSeries {
    @JsonProperty("Weekly Time Series")
    public Map<LocalDate, DayRecord> series;
}
