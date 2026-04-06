package com.example.reminder.sender;

import com.example.reminder.entity.NotificationChannel;
import com.example.reminder.entity.ReminderEntity;
import com.example.reminder.entity.ReminderStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WebhookNotificationSenderIntegrationTest {

    @Autowired
    private WebhookNotificationSender webhookNotificationSender;

    @Test
    void sendShouldReturnSuccessFor2xxAndFailureFor5xx() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(204));
            server.enqueue(new MockResponse().setResponseCode(500));
            server.start();

            String url = server.url("/hook").toString();

            ReminderEntity reminder = ReminderEntity.builder()
                    .id(42L)
                    .message("Оплатить подписку")
                    .subject("Напоминание")
                    .channel(NotificationChannel.WEBHOOK)
                    .recipient(url)
                    .scheduledAt(Instant.parse("2026-04-10T18:00:00Z"))
                    .status(ReminderStatus.READY_TO_SEND)
                    .attempts(0)
                    .build();

            SendResult successResult = webhookNotificationSender.send(reminder);
            SendResult failResult = webhookNotificationSender.send(reminder);

            assertThat(successResult.success()).isTrue();
            assertThat(failResult.success()).isFalse();
            assertThat(failResult.errorMessage()).contains("status code 500");
        }
    }
}
