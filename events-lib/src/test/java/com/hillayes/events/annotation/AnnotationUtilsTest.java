package com.hillayes.events.annotation;

import com.hillayes.events.domain.Topic;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnnotationUtilsTest {
    @Test
    public void testGetTopics() {
        // when: getTopics is called
        Collection<Topic> topics = AnnotationUtils.getTopics(new TopicConsumerAnnotatedClass());

        // then: the topics are returned
        assertEquals(2, topics.size());
        assertTrue(topics.contains(Topic.USER_AUTH));
        assertTrue(topics.contains(Topic.USER));
    }

    @Test
    public void testGetTopicsConsumed() {
        // when: getTopics is called
        Collection<Topic> topics = AnnotationUtils.getTopics(new TopicsConsumedAnnotatedClass());

        // then: the topics are returned
        assertEquals(2, topics.size());
        assertTrue(topics.contains(Topic.USER_AUTH));
        assertTrue(topics.contains(Topic.USER));
    }

    @Test
    public void testSubclass() {
        // when: getTopics is called
        Collection<Topic> topics = AnnotationUtils.getTopics(new SubClass());

        // then: the topics are returned
        assertEquals(4, topics.size());
        assertTrue(topics.contains(Topic.USER_AUTH));
        assertTrue(topics.contains(Topic.USER));
        assertTrue(topics.contains(Topic.RETRY_TOPIC));
        assertTrue(topics.contains(Topic.CONSENT));
    }

    @Test
    public void testConsumerGroup() {
        // when: getConsumerGroup is called
        Optional<String> consumerGroup = AnnotationUtils.getConsumerGroup(new TopicConsumerAnnotatedClass());

        // then: the consumer group is returned
        assertTrue(consumerGroup.isPresent());
        assertEquals("group1", consumerGroup.get());
    }

    @Test
    public void testConsumerGroupSubClass() {
        // when: getConsumerGroup is called
        Optional<String> consumerGroup = AnnotationUtils.getConsumerGroup(new SubClass());

        // then: the consumer group is returned
        assertTrue(consumerGroup.isPresent());
        assertEquals("group1", consumerGroup.get());
    }

    @Test
    public void testConsumerGroupSubClassGroup() {
        // when: getConsumerGroup is called
        Optional<String> consumerGroup = AnnotationUtils.getConsumerGroup(new GroupSubClass());

        // then: the consumer group is returned
        assertTrue(consumerGroup.isPresent());
        assertEquals("group2", consumerGroup.get());
    }

    @TopicConsumer(Topic.USER_AUTH)
    @TopicConsumer(Topic.USER)
    @ConsumerGroup("group1")
    public static class TopicConsumerAnnotatedClass {
    }

    @TopicsConsumed({
        @TopicConsumer(Topic.USER_AUTH),
        @TopicConsumer(Topic.USER)
    })
    public static class TopicsConsumedAnnotatedClass {
    }

    @TopicsConsumed({
        @TopicConsumer(Topic.RETRY_TOPIC),
        @TopicConsumer(Topic.CONSENT)
    })
    public static class SubClass extends TopicConsumerAnnotatedClass {}

    @ConsumerGroup("group2")
    public static class GroupSubClass extends TopicConsumerAnnotatedClass {}
}
