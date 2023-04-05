package com.hillayes.rail.model;

import lombok.Builder;

import java.util.List;

@Builder
public class TransactionList {
    public static final TransactionList NULL_LIST = TransactionList.builder()
        .booked(List.of())
        .pending(List.of())
        .build();

    public List<TransactionDetail> booked;
    public List<TransactionDetail> pending;
}
