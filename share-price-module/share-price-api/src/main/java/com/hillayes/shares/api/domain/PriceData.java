package com.hillayes.shares.api.domain;

import java.time.LocalDate;

public record PriceData(
    LocalDate date,
    Float open,
    Float high,
    Float low,
    Float close
) {}
