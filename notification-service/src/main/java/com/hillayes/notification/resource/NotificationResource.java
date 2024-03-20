package com.hillayes.notification.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.service.NotificationService;
import com.hillayes.onestop.api.NotificationResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/notifications")
@RolesAllowed({"user"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class NotificationResource {
    private static final NotificationService.NotificationMapper<NotificationResponse> MAPPER = new Mapper();

    private final NotificationService notificationService;

    @GET
    public Response getNotifications(@Context SecurityContext ctx,
                                     @QueryParam("after") Instant after) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting user notifications [userId: {}, after: {}]", userId, after);

        if (after == null) after = Instant.EPOCH;
        List<NotificationResponse> notifications = notificationService.listNotifications(userId, after, MAPPER);

        log.debug("Get user notifications [userId: {}, after: {}, count: {}]", userId, after, notifications.size());
        return Response.ok(notifications).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteNotification(@Context SecurityContext ctx,
                                       @PathParam("id") UUID notificationId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Deleting user notification [userId: {}, notificationId: {}]", userId, notificationId);

        notificationService.deleteNotification(userId, notificationId);
        log.debug("Deleted user notification [userId: {}, notificationId: {}]", userId, notificationId);
        return Response.noContent().build();
    }

    /**
     * An implementation of NotificationService.NotificationMapper to be passed to the service
     * when retrieving notifications. It allows the service to return both the notification
     * record and its rendered message.
     */
    private static class Mapper implements NotificationService.NotificationMapper<NotificationResponse> {
        public NotificationResponse map(Notification notification, String message) {
            return new NotificationResponse()
                .id(notification.getId())
                .correlationId(notification.getCorrelationId())
                .timestamp(notification.getDateCreated())
                .topic(notification.getMessageId().getTopic().name())
                .severity(NotificationResponse.SeverityEnum.fromValue(notification.getMessageId().getSeverity().name()))
                .message(message);
        }
    }
}
