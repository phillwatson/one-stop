package com.hillayes.events.annotation;

import com.hillayes.events.domain.Topic;

import java.lang.annotation.*;

/**
 * Annotates an implementation of EventConsumer to indicate the topic it consumes from.
 * This is a repeatable annotation, so a consumer may listen to multiple topics.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ConsumerTopics.class)
public @interface TopicConsumer {
    Topic value();
}
