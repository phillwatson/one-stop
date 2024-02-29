package com.hillayes.user.domain;

import com.hillayes.commons.jpa.LocaleAttrConverter;
import com.hillayes.openid.AuthProvider;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
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

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "password_last_set")
    private Instant passwordLastSet;

    @ColumnTransformer(write = "LOWER(?)")
    private String email;

    private String title;

    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Column(name = "family_name", nullable = true)
    private String familyName;

    @Column(name="preferred_name", nullable = true)
    private String preferredName;

    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    /**
     * Indicates the natural language and locale that the user prefers.
     */
    @Column(name = "locale", nullable = true)
    @Convert(converter = LocaleAttrConverter.class)
    private Locale locale;

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @Column(name = "date_onboarded")
    private Instant dateOnboarded;

    @Transient
    public boolean isOnboarded() {
        return dateOnboarded != null;
    }

    @Column(name="date_blocked")
    private Instant dateBlocked;

    @Transient
    public boolean isBlocked() {
        return dateBlocked != null;
    }

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
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @Transient
    public OidcIdentity addOidcIdentity(AuthProvider provider, String issuer, String subject) {
        OidcIdentity result = OidcIdentity.builder()
            .user(this)
            .provider(provider)
            .issuer(issuer)
            .subject(subject)
            .build();
        oidcIdentities.add(result);
        return result;
    }

    @Transient
    public Optional<OidcIdentity> removeOidcIdentity(AuthProvider provider) {
        Optional<OidcIdentity> result = oidcIdentities.stream()
            .filter(oidc -> oidc.getProvider().equals(provider))
            .findFirst();
        result.ifPresent(oidcIdentities::remove);
        return result;
    }

    @Transient
    public Optional<OidcIdentity> getOidcIdentity(String issuer) {
        return oidcIdentities.stream()
            .filter(oidc -> oidc.getIssuer().equals(issuer))
            .findFirst();
    }
}
