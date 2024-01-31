package com.hillayes.rail.api.domain;

import lombok.*;

import java.util.List;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class RailInstitution implements Comparable<RailInstitution> {
    @EqualsAndHashCode.Include
    private String id;

    @ToString.Include
    private RailProvider provider;

    @ToString.Include
    private String name;

    @ToString.Include
    private String bic;

    private String logo;

    private List<String> countries;

    private int transactionTotalDays;

    private boolean paymentsEnabled;

    @Override
    public int compareTo(RailInstitution other) {
        if (other == this) {
            return 0;
        }
        if (other == null) {
            return 1;
        }

        return this.name.compareTo(other.name);
    }
}
