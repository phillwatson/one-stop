package com.hillayes.rail.shares;

import java.time.LocalDate;

public record DailyPrice (
    LocalDate date,
    Float open,
    Float high,
    Float low,
    Float close
) {}
