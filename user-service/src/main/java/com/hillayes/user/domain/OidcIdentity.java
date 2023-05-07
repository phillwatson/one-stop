package com.hillayes.user.domain;

import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class OidcIdentity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @ToString.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @EqualsAndHashCode.Include
    @Column(name = "issuer", nullable = false)
    private String issuer;

    @ToString.Include
    @Column(name = "subject", nullable = false)
    private String subject;

    @Builder.Default
    @ToString.Include
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @ToString.Include
    @Column(name = "date_disabled", nullable = true)
    private Instant dateDisabled;

    @Transient
    public boolean isDisabled() {
        return dateDisabled != null;
    }
}

