package com.example.reminder.exception;

import com.example.reminder.entity.NotificationChannel;

public class SenderNotConfiguredException extends RuntimeException {

    public SenderNotConfiguredException(NotificationChannel channel) {
        super("Sender is not configured for channel: " + channel);
    }
}
