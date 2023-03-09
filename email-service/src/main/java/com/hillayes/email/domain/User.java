package com.hillayes.email.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.*;
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
    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Setter
    @Column(name = "family_name", nullable = false)
    private String familyName;

    @Version
    @JsonIgnore
    private Integer version;
}
