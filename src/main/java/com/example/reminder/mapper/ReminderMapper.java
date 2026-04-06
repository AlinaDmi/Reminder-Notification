package com.example.reminder.mapper;

import com.example.reminder.dto.CreateReminderRequest;
import com.example.reminder.dto.CreateReminderResponse;
import com.example.reminder.dto.ReminderResponse;
import com.example.reminder.entity.ReminderEntity;
import com.example.reminder.entity.ReminderStatus;
import org.springframework.stereotype.Component;

@Component
public class ReminderMapper {

    public ReminderEntity toEntity(CreateReminderRequest request) {
        return ReminderEntity.builder()
                .message(request.getMessage())
                .subject(request.getSubject())
                .channel(request.getChannel())
                .recipient(request.getRecipient())
                .scheduledAt(request.getScheduledAt())
                .status(ReminderStatus.CREATED)
                .attempts(0)
                .lastError(null)
                .nextRetryAt(request.getScheduledAt())
                .build();
    }

    public CreateReminderResponse toCreateResponse(ReminderEntity entity) {
        return CreateReminderResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .build();
    }

    public ReminderResponse toResponse(ReminderEntity entity) {
        return ReminderResponse.builder()
                .id(entity.getId())
                .message(entity.getMessage())
                .subject(entity.getSubject())
                .channel(entity.getChannel())
                .recipient(entity.getRecipient())
                .scheduledAt(entity.getScheduledAt())
                .status(entity.getStatus())
                .attempts(entity.getAttempts())
                .lastError(entity.getLastError())
                .build();
    }
}
