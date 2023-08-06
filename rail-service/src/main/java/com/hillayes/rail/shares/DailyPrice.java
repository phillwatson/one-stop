package com.hillayes.rail.shares;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;

@Builder
@Getter
@ToString
public class DailyPrice {
    private final LocalDate date;
    private final Float open;
    private final Float high;
    private final Float low;
    private final Float close;
}
