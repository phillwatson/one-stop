package com.hillayes.rail.api.domain;

import lombok.*;

import java.util.Currency;
import java.util.List;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class RailAccount {
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @ToString.Include
    private String institutionId;

    @ToString.Include
    private String name;

    private String ownerName;

    private String iban;

    private AccountStatus status;

    private String accountType;

    private Currency currency;

    private RailBalance balance;
}
