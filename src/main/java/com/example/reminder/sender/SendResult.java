package com.example.reminder.sender;

public record SendResult(boolean success, String errorMessage) {

    public static SendResult ok() {
        return new SendResult(true, null);
    }

    public static SendResult fail(String errorMessage) {
        return new SendResult(false, errorMessage);
    }
}
