package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.JobbingTaskData;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PollAccountJobbingTaskPayloadTest {
    @Test
    public void testSerialization() {
        // given: a poll-account jobbing task payload
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(
            UUID.randomUUID(),
            UUID.randomUUID().toString());

        // and: the payload is serialized
        JobbingTaskData taskData = new JobbingTaskData(UUID.randomUUID().toString(), payload);

        // when: the payload is deserialized
        PollAccountJobbingTask.Payload payloadContent = taskData.getPayloadContent();

        // the consent ID is correct
        assertNotNull(payloadContent.consentId());
        assertEquals(payload.consentId(), payloadContent.consentId());

        // and: the rail-account ID is correct
        assertNotNull(payloadContent.railAccountId());
        assertEquals(payload.railAccountId(), payloadContent.railAccountId());
    }
}
