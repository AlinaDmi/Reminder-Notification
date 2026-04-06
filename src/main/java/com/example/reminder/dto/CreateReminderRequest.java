package com.example.reminder.dto;

import com.example.reminder.entity.NotificationChannel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.time.Instant;
import java.util.regex.Pattern;

@Getter
@Setter
public class CreateReminderRequest {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @NotBlank
    private String message;

    private String subject;

    @NotNull
    private NotificationChannel channel;

    @NotBlank
    private String recipient;

    @NotNull
    @Future
    private Instant scheduledAt;

    @AssertTrue(message = "recipient does not match channel requirements")
    public boolean isRecipientValid() {
        if (channel == null || recipient == null || recipient.isBlank()) {
            return true;
        }
        return switch (channel) {
            case WEBHOOK -> isValidWebhookUrl(recipient);
            case EMAIL -> EMAIL_PATTERN.matcher(recipient).matches();
            case LOG -> true;
        };
    }

    private boolean isValidWebhookUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null &&
                    ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (Exception ex) {
            return false;
        }
    }
}
