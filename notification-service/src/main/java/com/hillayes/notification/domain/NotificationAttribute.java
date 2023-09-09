package com.hillayes.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notification_attribute")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class NotificationAttribute {
    @Id
    @GeneratedValue(generator = "uuid2")
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne()
    private Notification notification;

    @Column(name = "attr_name", nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private String name;

    @Column(name = "attr_value", nullable = false)
    @EqualsAndHashCode.Include
    private String value;
}
