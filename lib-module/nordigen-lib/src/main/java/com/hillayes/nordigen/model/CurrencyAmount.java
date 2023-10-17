package com.hillayes.nordigen.model;

import lombok.Builder;

@Builder
public class CurrencyAmount {
    public Float amount;
    public String currency;
}
