package com.example.reminder.exception;

public class ReminderNotFoundException extends RuntimeException {

    public ReminderNotFoundException(Long id) {
        super("Reminder not found: " + id);
    }
}
