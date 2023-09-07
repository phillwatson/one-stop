package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.AccountActivity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@TopicConsumer(Topic.USER_AUTH)
@RequiredArgsConstructor
@Slf4j
public class UserAuthTopicConsumer implements EventConsumer {
    @Transactional
    public void consume(EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received auth event [payloadClass: {}]", payloadClass);

        Map<String, Object> params = new HashMap<>();
        if (AccountActivity.class.getName().equals(payloadClass)) {
            AccountActivity event = eventPacket.getPayloadContent();
        }
    }
}
