package com.hillayes.notification.domain;

import com.hillayes.commons.Strings;
import com.hillayes.events.domain.Topic;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Topic topic;

    @ToString.Include
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationMessageId messageId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "notification_id")
    @Builder.Default
    private Set<NotificationAttribute> attributes = new HashSet<>();

    public boolean addAttr(String name, String value) {
        if (Strings.isBlank(name)) {
            return false;
        }

        NotificationAttribute attr = NotificationAttribute.builder()
            .notification(this)
            .name(name.toLowerCase())
            .value(value)
            .build();

        if (Strings.isBlank(value)) {
            return getAttributes().remove(attr);
        }
        return getAttributes().add(attr);
    }
}
