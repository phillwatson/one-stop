package com.hillayes.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(generator = "uuid2")
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
    @Column(name = "message_id", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationId messageId;

    @Column(nullable = true)
    private String attributes;
}
