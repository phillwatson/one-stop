package com.hillayes.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "notification_attribute")
@IdClass(NotificationAttribute.AttributeId.class)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class NotificationAttribute {
    @Id
    @ManyToOne(optional = false)
    private Notification notification;

    @Id
    @Column(name = "attr_name", nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private String name;

    @Column(name = "attr_value", nullable = false)
    @EqualsAndHashCode.Include
    private String value;

    @Data
    public static class AttributeId implements Serializable {
        private Notification notification;
        private String name;
    }
}
