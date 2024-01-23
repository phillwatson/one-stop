package com.hillayes.rail.api.domain;

import lombok.*;

import java.util.Currency;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Account {
    private String id;

    private AccountStatus status;

    private String name;

    private String institutionId;

    private String iban;

    private String accountType;

    private String ownerName;

    private Currency currency;
}
