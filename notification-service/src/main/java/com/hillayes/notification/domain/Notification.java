package com.hillayes.notification.domain;

import com.hillayes.events.domain.Topic;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Notification {
    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID id;

    @ToString.Include
    @EqualsAndHashCode.Include
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "date_created", nullable = false)
    private Instant dateCreated;

    @ToString.Include
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Topic topic;

    private String message;
}
