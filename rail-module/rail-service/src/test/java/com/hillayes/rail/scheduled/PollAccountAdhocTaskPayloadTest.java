package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.AdhocTaskData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PollAccountAdhocTaskPayloadTest {
    @Test
    public void testSerialization() {
        // given: a poll-account adhoc task payload
        PollAccountAdhocTask.Payload payload = new PollAccountAdhocTask.Payload(
            UUID.randomUUID(),
            UUID.randomUUID().toString());

        // and: the payload is serialized
        AdhocTaskData taskData = new AdhocTaskData(UUID.randomUUID().toString(), payload);

        // when: the payload is deserialized
        PollAccountAdhocTask.Payload payloadContent = taskData.getPayloadContent();

        // the consent ID is correct
        assertNotNull(payloadContent.consentId());
        assertEquals(payload.consentId(), payloadContent.consentId());

        // and: the rail-account ID is correct
        assertNotNull(payloadContent.railAccountId());
        assertEquals(payload.railAccountId(), payloadContent.railAccountId());
    }
}
