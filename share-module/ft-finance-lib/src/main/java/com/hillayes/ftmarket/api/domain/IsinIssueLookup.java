package com.hillayes.ftmarket.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity(name = "isin_issue_lookup")
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

    @ToString.Include
    @Column(name = "name")
    private String name;

    @ToString.Include
    @Column(name = "currency_code")
    private String currencyCode;
}
