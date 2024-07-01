package com.hillayes.rail.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "category_selector")
@Getter
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class CategorySelector {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @Version
    @Column(name = "version")
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    private Category category;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "regex", nullable = false)
    private String regex;
}
