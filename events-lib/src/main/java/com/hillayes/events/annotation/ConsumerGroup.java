package com.hillayes.events.annotation;

import java.lang.annotation.*;

/**
 * Annotates an implementation of EventConsumer to indicate the consumer group it belongs to.
 * By default, the consumer group is taken from the configuration property "kafka.group.id",
 * or the application/service name if that property is not defined.
 *
 * By specifying an explicit consumer group, consumers across service boundaries can share
 * messages from the same topic(s).
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsumerGroup {
    String value();
}
