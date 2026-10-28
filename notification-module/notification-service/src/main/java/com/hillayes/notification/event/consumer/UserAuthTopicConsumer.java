package com.hillayes.notification.event.consumer;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.annotation.TopicObserver;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.auth.NewAuthProvider;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.task.SendEmailTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserAuthTopicConsumer {
    private final SendEmailTask sendEmailTask;

    @TopicObserver
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void consume(@Observes
                        @TopicObserved(Topic.USER_AUTH) EventPacket eventPacket) {
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
        sendEmailTask.queueTask(event.getUserId(), TemplateName.NEW_OIDC_LOGIN, params);
    }
}
