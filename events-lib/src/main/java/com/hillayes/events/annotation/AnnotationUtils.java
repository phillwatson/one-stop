package com.hillayes.events.annotation;

import com.hillayes.events.domain.Topic;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AnnotationUtils {
    public static Collection<Topic> getTopics(Object instance) {
        return getRepeatedAnnotations(instance.getClass(), ConsumerTopic.class, new HashSet<>())
                .stream()
                .map(ConsumerTopic::value)
                .collect(Collectors.toSet());
    }

    public static Optional<String> getConsumerGroup(Object instance) {
        ConsumerGroup consumerGroup = getFirstAnnotation(instance.getClass(), ConsumerGroup.class);
        return Optional.ofNullable(consumerGroup != null ? consumerGroup.value() : null);
    }

    public static <T extends Annotation> T getFirstAnnotation(Class<?> clazz, Class<T> annotationClass) {
        if (clazz != null) {
            T annotation = clazz.getAnnotation(annotationClass);
            if (annotation != null)
                return annotation;
            return getFirstAnnotation(clazz.getSuperclass(), annotationClass);
        }
        return null;
    }

    public static <T extends Annotation> Collection<T> getRepeatedAnnotations(Class<?> clazz,
                                                                              Class<T> annotationClass,
                                                                              Collection<T> annotations) {
        if (clazz != null) {
            Collections.addAll(annotations, clazz.getAnnotationsByType(annotationClass));
            annotations.addAll(getRepeatedAnnotations(clazz.getSuperclass(), annotationClass, annotations));
        }
        return annotations;
    }
}
