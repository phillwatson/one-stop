package com.hillayes.alphavantage.api;

public enum TimeSeries {
    TIME_SERIES_INTRADAY,
    TIME_SERIES_DAILY,
    TIME_SERIES_DAILY_ADJUSTED, // Premium account required
    TIME_SERIES_WEEKLY,
    TIME_SERIES_WEEKLY_ADJUSTED,
    TIME_SERIES_MONTHLY,
    TIME_SERIES_MONTHLY_ADJUSTED,

    // returns the latest price and volume information for chosen symbol.
    GLOBAL_QUOTE;
}
