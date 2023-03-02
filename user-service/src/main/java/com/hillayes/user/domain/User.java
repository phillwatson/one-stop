package com.hillayes.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    @Id
    @GeneratedValue(generator = "uuid2")
    @ToString.Include
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Setter
    @Column(nullable = false)
    private String email;

    @Setter
    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Setter
    @Column(name = "family_name", nullable = false)
    private String familyName;

    @Setter
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Builder.Default
    @Column(name = "date_created", nullable = false)
    private Instant dateCreated = Instant.now();

    @Setter
    @Column(name = "date_onboarded")
    private Instant dateOnboarded;

    @Setter
    @Column(name="deleted")
    private boolean deleted;

    @Setter
    @Column(name="blocked_on")
    private Instant blockedOn;

    @Setter
    @Column(name="blocked_reason")
    private Instant blockedReason;

    @Version
    @JsonIgnore
    private Integer version;

    @ElementCollection
    @CollectionTable(name="userrole", joinColumns=@JoinColumn(name="user_id"))
    @Column(name="role")
    private Set<String> roles = new HashSet<>();

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    @JsonProperty
    protected void setPasswordHash(String aValue) {
        passwordHash = aValue;
    }
}
