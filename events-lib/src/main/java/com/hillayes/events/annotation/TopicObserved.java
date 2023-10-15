package com.hillayes.events.annotation;

import com.hillayes.events.domain.Topic;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an implementation of EventConsumer to indicate the topic it consumes from.
 * This is a repeatable annotation, so a consumer may listen to multiple topics.
 */
@Qualifier
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TopicObserved {
    Topic[] value();
}
