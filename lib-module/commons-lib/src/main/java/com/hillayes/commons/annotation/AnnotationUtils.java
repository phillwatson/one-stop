package com.hillayes.commons.annotation;

import java.lang.annotation.Annotation;
import java.util.*;

public final class AnnotationUtils {
    public static <T extends Annotation> Optional<T> getFirstAnnotation(Class<?> clazz, Class<T> annotationClass) {
        if (clazz != null) {
            T annotation = clazz.getAnnotation(annotationClass);
            if (annotation != null)
                return Optional.of(annotation);
            return getFirstAnnotation(clazz.getSuperclass(), annotationClass);
        }
        return Optional.empty();
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
