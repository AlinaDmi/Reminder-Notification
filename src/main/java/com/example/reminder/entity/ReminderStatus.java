package com.example.reminder.entity;

public enum ReminderStatus {
    CREATED,
    READY_TO_SEND,
    PROCESSING,
    SENT,
    RETRY_PENDING,
    FAILED,
    CANCELLED
}
