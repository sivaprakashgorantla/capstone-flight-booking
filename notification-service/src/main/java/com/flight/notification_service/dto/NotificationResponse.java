package com.flight.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    private Long id;
    private String notificationReference;
    private String userId;
    private String userEmail;

    private String type;        // NotificationType name
    private String channel;     // NotificationChannel name
    private String status;      // NotificationStatus name

    private String title;
    private String message;

    private String relatedEntityId;
    private String relatedEntityType;

    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    /** Populated only on action responses */
    private String responseMessage;
}
