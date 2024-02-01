package com.hillayes.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a pending notification issued to a user. The client can poll for
 * notifications and delete them once read. The messageId identifies a
 * template, and the attributes are the context parameters to be used when
 * rendering the message.
 */
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

    @ToString.Include
    @Column(name = "correlation_id", nullable = true)
    private String correlationId;

    @Column(name = "date_created", nullable = false)
    private Instant dateCreated;

    /**
     * Identifies the notification's message template. The template may
     * have multiple texts, from which one is selected based on the user's
     * locale.
     */
    @ToString.Include
    @Column(name = "message_id", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationId messageId;

    /**
     * The context attributes to be used when rendering the message template.
     */
    @Column(nullable = true)
    private String attributes;
}
