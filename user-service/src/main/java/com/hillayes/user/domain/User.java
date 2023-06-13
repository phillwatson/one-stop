package com.hillayes.user.domain;

import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(generator = "uuid2")
    @ToString.Include
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Setter
    @Column(nullable = false)
    @ColumnTransformer(write = "LOWER(?)")
    private String email;

    @Setter
    @Column(name="title", nullable = true)
    private String title;

    @Setter
    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Setter
    @Column(name = "family_name", nullable = true)
    private String familyName;

    @Setter
    @Column(name="preferred_name", nullable = true)
    private String preferredName;

    @Setter
    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @Setter
    @Column(name = "date_onboarded")
    private Instant dateOnboarded;

    @Transient
    public boolean isOnboarded() {
        return dateOnboarded != null;
    }

    @Setter
    @Column(name="date_blocked")
    private Instant dateBlocked;

    @Transient
    public boolean isBlocked() {
        return dateBlocked != null;
    }

    @Setter
    @Column(name="blocked_reason")
    private Instant blockedReason;

    @Builder.Default
    @OneToMany(mappedBy = "user", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<OidcIdentity> oidcIdentities = new HashSet<>();

    @Version
    private Integer version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="userrole", joinColumns=@JoinColumn(name="user_id"))
    @Column(name="role")
    private Set<String> roles = new HashSet<>();

    public String getPasswordHash() {
        return passwordHash;
    }

    protected void setPasswordHash(String aValue) {
        passwordHash = aValue;
    }

    @Transient
    public OidcIdentity addOidcIdentity(String issuer, String subject) {
        OidcIdentity result = OidcIdentity.builder()
            .user(this)
            .issuer(issuer)
            .subject(subject)
            .build();
        oidcIdentities.add(result);
        return result;
    }

    @Transient
    public Optional<OidcIdentity> getOidcIdentity(String issuer) {
        return oidcIdentities.stream()
            .filter(oidc -> oidc.getIssuer().equals(issuer))
            .findFirst();
    }
}
