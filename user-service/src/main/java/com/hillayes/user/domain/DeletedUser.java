package com.hillayes.user.domain;

import com.hillayes.commons.jpa.LocaleAttrConverter;
import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "deleted_user")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class DeletedUser {
    @Id
    @ToString.Include
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    private String email;

    private String title;

    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Column(name = "family_name", nullable = true)
    private String familyName;

    @Column(name = "preferred_name", nullable = true)
    private String preferredName;

    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    /**
     * Indicates the natural language and locale that the user prefers.
     */
    @Convert(converter = LocaleAttrConverter.class)
    private Locale locale;

    @Column(name = "date_created", nullable = false)
    private Instant dateCreated;

    @Column(name = "date_onboarded")
    private Instant dateOnboarded;

    @Column(name = "date_deleted")
    private Instant dateDeleted;

    @Column(name = "date_blocked")
    private Instant dateBlocked;

    @Column(name = "blocked_reason")
    private Instant blockedReason;

    /**
     * This is used to copy the version of the original user, but should not
     * be used for optimistic locking of this deleted user entity.
     */
    private Integer version;

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
            .locale(user.getLocale())
            .dateCreated(user.getDateCreated())
            .dateOnboarded(user.getDateOnboarded())
            .dateBlocked(user.getDateBlocked())
            .blockedReason(user.getBlockedReason())
            .version(user.getVersion())
            .build();
    }
}
