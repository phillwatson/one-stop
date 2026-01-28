package com.hillayes.shares.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolio")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Portfolio {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Setter
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "name", nullable = false)
    private String name;

    @lombok.Builder.Default
    @ToString.Include
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @Formula("(select count(1) from shares.holding h where h.portfolio_id = id)")
    private int holdingCount;
}
