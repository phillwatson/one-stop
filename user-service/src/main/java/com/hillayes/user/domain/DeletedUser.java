package com.hillayes.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "deleted_user")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeletedUser {
    @Id
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
    @Column(name = "title", nullable = true)
    private String title;

    @Setter
    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Setter
    @Column(name = "family_name", nullable = true)
    private String familyName;

    @Setter
    @Column(name = "preferred_name", nullable = true)
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
    @JsonIgnore
    public boolean isOnboarded() {
        return dateOnboarded != null;
    }

    @Setter
    @Column(name = "date_deleted")
    private Instant dateDeleted;

    @Setter
    @Column(name = "date_blocked")
    private Instant dateBlocked;

    @Transient
    @JsonIgnore
    public boolean isBlocked() {
        return dateBlocked != null;
    }

    @Setter
    @Column(name = "blocked_reason")
    private Instant blockedReason;

    @Version
    @JsonIgnore
    private Integer version;

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    @JsonProperty
    protected void setPasswordHash(String aValue) {
        passwordHash = aValue;
    }

    public static DeletedUser fromUser(User user) {
        return DeletedUser.builder()
            .dateDeleted(Instant.now())
            .id(user.getId())
            .username(user.getUsername())
            .passwordHash(user.getPasswordHash())
            .email(user.getEmail())
            .title(user.getTitle())
            .givenName(user.getGivenName())
            .familyName(user.getFamilyName())
            .preferredName(user.getPreferredName())
            .phoneNumber(user.getPhoneNumber())
            .dateCreated(user.getDateCreated())
            .dateOnboarded(user.getDateOnboarded())
            .dateBlocked(user.getDateBlocked())
            .blockedReason(user.getBlockedReason())
            .version(user.getVersion())
            .build();
    }
}
