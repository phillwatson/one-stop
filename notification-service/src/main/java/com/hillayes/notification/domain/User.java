package com.hillayes.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hillayes.commons.jpa.LocaleAttrConverter;
import lombok.*;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    @Id
    @ToString.Include
    private UUID id;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false)
    private String username;

    @Setter
    @Column(nullable = false)
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

    /**
     * Indicates the natural language and locale that the user prefers.
     */
    @Setter
    @Column(name = "locales", nullable = true)
    @Convert(converter = LocaleAttrConverter.class)
    private Locale locale;

    @Column(name = "date_created", nullable = true)
    private Instant dateCreated;

    @Column(name = "date_updated", nullable = true)
    private Instant dateUpdated;

    @Version
    @JsonIgnore
    private Integer version;
}
