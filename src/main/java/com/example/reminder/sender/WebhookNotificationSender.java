package com.example.reminder.sender;

import com.example.reminder.config.ReminderProperties;
import com.example.reminder.entity.NotificationChannel;
import com.example.reminder.entity.ReminderEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class WebhookNotificationSender implements NotificationSender {

    private final ObjectMapper objectMapper;
    private final ReminderProperties reminderProperties;
    private final HttpClient httpClient;

    public WebhookNotificationSender(ObjectMapper objectMapper, ReminderProperties reminderProperties) {
        this.objectMapper = objectMapper;
        this.reminderProperties = reminderProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(reminderProperties.getWebhookTimeoutMs()))
                .build();
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WEBHOOK;
    }

    @Override
    public SendResult send(ReminderEntity reminder) {
        try {
            String payload = toPayload(reminder);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(reminder.getRecipient()))
                    .timeout(Duration.ofMillis(reminderProperties.getWebhookTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                log.info("WEBHOOK success reminderId={} statusCode={}", reminder.getId(), statusCode);
                return SendResult.ok();
            }
            return SendResult.fail("Webhook returned status code " + statusCode);
        } catch (IllegalArgumentException ex) {
            return SendResult.fail("Invalid webhook URL");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return SendResult.fail("Webhook call interrupted");
        } catch (IOException ex) {
            return SendResult.fail(ex.getMessage());
        }
    }

    private String toPayload(ReminderEntity reminder) throws JsonProcessingException {
        Map<String, Object> body = Map.of(
                "reminderId", reminder.getId(),
                "message", reminder.getMessage(),
                "subject", reminder.getSubject(),
                "channel", reminder.getChannel().name(),
                "scheduledAt", reminder.getScheduledAt().toString()
        );
        return objectMapper.writeValueAsString(body);
    }
}
