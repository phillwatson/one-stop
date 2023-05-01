package com.hillayes.events.consumer;

import com.hillayes.events.domain.Topic;

import java.lang.annotation.*;

/**
 * Annotates an implementation of {@link EventConsumer} to indicate the topic it consumes from.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ConsumerTopics.class)
public @interface ConsumerTopic {
    Topic topic();
}
