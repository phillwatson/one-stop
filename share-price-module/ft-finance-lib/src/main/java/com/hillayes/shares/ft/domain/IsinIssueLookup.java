package com.hillayes.shares.ft.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class IsinIssueLookup {
    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "isin", nullable = false)
    private String isin;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "issue_id", nullable = false)
    private String issueId;
}
