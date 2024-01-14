package com.hillayes.events.annotation;

import com.hillayes.events.domain.Topic;

import java.util.*;
import java.util.stream.Collectors;

import static com.hillayes.commons.annotation.AnnotationUtils.getFirstAnnotation;
import static com.hillayes.commons.annotation.AnnotationUtils.getRepeatedAnnotations;

public final class AnnotationUtils {
    public static Collection<Topic> getTopics(Object instance) {
        Set<Topic> result = getRepeatedAnnotations(instance.getClass(), TopicConsumer.class, new HashSet<>())
                .stream()
                .map(TopicConsumer::value)
                .collect(Collectors.toSet());

        getRepeatedAnnotations(instance.getClass(), TopicsConsumed.class, new HashSet<>())
                .stream()
                .map(TopicsConsumed::value)
                .flatMap(Arrays::stream)
                .map(TopicConsumer::value)
                .forEach(result::add);

        return result;
    }

    public static Optional<String> getConsumerGroup(Object instance) {
        return getFirstAnnotation(instance.getClass(), ConsumerGroup.class)
            .map(ConsumerGroup::value);
    }
}
