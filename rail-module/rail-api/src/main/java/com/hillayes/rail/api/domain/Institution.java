package com.hillayes.rail.api.domain;

import lombok.*;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Institution implements Comparable<Institution> {
    @EqualsAndHashCode.Include
    private String id;

    @ToString.Include
    private String name;

    @ToString.Include
    private String bic;

    private String logo;

    private List<String> countries;

    private int transactionTotalDays;

    private boolean paymentsEnabled;

    @Override
    public int compareTo(Institution other) {
        if (other == this) {
            return 0;
        }
        if (other == null) {
            return 1;
        }

        return this.name.compareTo(other.name);
    }
}
