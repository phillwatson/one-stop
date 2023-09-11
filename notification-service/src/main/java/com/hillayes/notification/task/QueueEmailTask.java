package com.hillayes.notification.task;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.service.SendEmailService;
import com.hillayes.notification.service.UserService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class QueueEmailTask extends AbstractNamedJobbingTask<QueueEmailTask.Payload> {
    private final UserService userService;
    private final SendEmailService sendEmailService;

    public QueueEmailTask(UserService userService,
                          SendEmailService sendEmailService) {
        super("queue-email");
        this.userService = userService;
        this.sendEmailService = sendEmailService;
    }

    public String queueJob(UUID userId,
                           TemplateName templateName,
                           Map<String, Object> params) {
        log.info("Queuing email job [template: {}]", templateName);
        Payload payload = Payload.builder()
            .userId(userId)
            .templateName(templateName)
            .params(params)
            .build();
        return scheduler.addJob(this, payload);
    }

    public String queueJob(SendEmailService.Recipient recipient,
                           TemplateName templateName,
                           Map<String, Object> params) {
        log.info("Queuing email job [template: {}]", templateName);
        Payload payload = Payload.builder()
            .recipient(recipient)
            .templateName(templateName)
            .params(params)
            .build();
        return scheduler.addJob(this, payload);
    }

    @Transactional
    @Override
    public TaskConclusion apply(TaskContext<Payload> payloadTaskContext) {
        Payload payload = payloadTaskContext.getPayload();

        Map<String, Object> params = new HashMap<>();

        SendEmailService.Recipient recipient = payload.recipient;
        if (payload.userId != null) {
            User user = userService.getUser(payload.userId).orElse(null);
            if (user != null) {
                params.put("user", user);
            }

            if (payload.recipient == null) {
                recipient = new SendEmailService.Recipient(user);
            }
        }

        if (payload.params != null) {
            params.putAll(payload.params);
        }

        sendEmailService.sendEmail(payload.templateName, recipient, params);
        return TaskConclusion.COMPLETE;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @RegisterForReflection
    public static class Payload {
        UUID userId;
        SendEmailService.Recipient recipient;
        TemplateName templateName;
        Map<String, Object> params;
    }
}
