package com.example.reminder.dto;

import com.example.reminder.entity.NotificationChannel;
import com.example.reminder.entity.ReminderStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ReminderResponse {
    Long id;
    String message;
    String subject;
    NotificationChannel channel;
    String recipient;
    Instant scheduledAt;
    ReminderStatus status;
    int attempts;
    String lastError;
}
