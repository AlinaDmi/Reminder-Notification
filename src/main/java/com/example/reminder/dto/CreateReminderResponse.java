package com.example.reminder.dto;

import com.example.reminder.entity.ReminderStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateReminderResponse {
    Long id;
    ReminderStatus status;
}
