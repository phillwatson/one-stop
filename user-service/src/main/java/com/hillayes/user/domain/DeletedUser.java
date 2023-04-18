package com.hillayes.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    @Column(nullable = false)
    private String email;

    @Column(name = "title", nullable = true)
    private String title;

    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Column(name = "family_name", nullable = true)
    private String familyName;

    @Column(name = "preferred_name", nullable = true)
    private String preferredName;

    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    @Column(name = "date_created", nullable = false)
    private Instant dateCreated;

    @Column(name = "date_onboarded")
    private Instant dateOnboarded;

    @Transient
    @JsonIgnore
    public boolean isOnboarded() {
        return dateOnboarded != null;
    }

    @Column(name = "date_deleted")
    private Instant dateDeleted;

    @Column(name = "date_blocked")
    private Instant dateBlocked;

    @Transient
    @JsonIgnore
    public boolean isBlocked() {
        return dateBlocked != null;
    }

    @Column(name = "blocked_reason")
    private Instant blockedReason;

    @Version
    @JsonIgnore
    private Integer version;

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
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