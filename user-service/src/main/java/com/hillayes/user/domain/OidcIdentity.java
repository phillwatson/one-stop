package com.hillayes.user.domain;

import com.hillayes.openid.AuthProvider;
import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
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
    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

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
    @Setter
    @Column(name = "date_last_used")
    private Instant dateLastUsed;

    @ToString.Include
    @Column(name = "date_disabled")
    private Instant dateDisabled;

    @Transient
    public boolean isDisabled() {
        return dateDisabled != null;
    }

    @Transient
    public void setDisabled(boolean value) {
        dateDisabled = (value) ? Instant.now() : null;
    }
}

