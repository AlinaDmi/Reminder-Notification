package com.example.reminder.exception;

public class InvalidReminderStateException extends RuntimeException {

    public InvalidReminderStateException(String message) {
        super(message);
    }
}
