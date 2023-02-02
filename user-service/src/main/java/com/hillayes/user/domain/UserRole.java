package com.hillayes.user.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.UUID;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRole {
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false)
    private UUID userId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(nullable = false)
    private String role;
}
