package com.hillayes.rail.api.domain;

import lombok.*;

import java.util.Currency;

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

    private AccountStatus status;

    @ToString.Include
    private String name;

    @ToString.Include
    private String institutionId;

    private String iban;

    private String accountType;

    private String ownerName;

    private Currency currency;
}
