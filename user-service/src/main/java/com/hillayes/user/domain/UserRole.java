package com.hillayes.user.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRole implements Serializable {
    @Id
    @Column(name = "user_id", nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID userId;

    @Id
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String role;
}
