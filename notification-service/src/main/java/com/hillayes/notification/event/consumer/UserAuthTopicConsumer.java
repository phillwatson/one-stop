package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.NewAuthProvider;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.task.SendEmailTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@ApplicationScoped
@TopicConsumer(Topic.USER_AUTH)
@RequiredArgsConstructor
@Slf4j
public class UserAuthTopicConsumer implements EventConsumer {
    private final SendEmailTask sendEmailTask;

    @Transactional
    public void consume(EventPacket eventPacket) {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user event [payloadClass: {}]", payloadClass);

        if (NewAuthProvider.class.getName().equals(payloadClass)) {
            processNewAuthProvider(eventPacket.getPayloadContent());
        }
    }

    private void processNewAuthProvider(NewAuthProvider event) {
        Map<String, Object> params = Map.of(
            "event", event
        );
        sendEmailTask.queueJob(event.getUserId(), TemplateName.NEW_OIDC_LOGIN, params);
    }
}
