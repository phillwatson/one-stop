package com.hillayes.shares.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "share_holding")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ShareHolding {
    @Id
    @GeneratedValue(generator = "uuid2")
    @Setter
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "share_index_id", nullable = false)
    private UUID shareIndexId;

    @Builder.Default
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "date_created", nullable = false)
    protected Instant dateCreated = Instant.now();
}
