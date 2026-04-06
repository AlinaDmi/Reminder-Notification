package com.example.reminder.service;

import com.example.reminder.config.ReminderProperties;
import com.example.reminder.entity.NotificationChannel;
import com.example.reminder.entity.ReminderEntity;
import com.example.reminder.entity.ReminderStatus;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.sender.NotificationSender;
import com.example.reminder.sender.SendResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderDispatchServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private NotificationSender webhookSender;

    @Mock
    private NotificationSender logSender;

    @Test
    void dispatchShouldUseSenderMatchingReminderChannel() {
        ReminderProperties properties = new ReminderProperties();
        properties.setMaxAttempts(3);
        properties.setRetryDelaySeconds(60);

        Clock clock = Clock.fixed(Instant.parse("2026-04-06T10:00:00Z"), ZoneOffset.UTC);

        when(webhookSender.getChannel()).thenReturn(NotificationChannel.WEBHOOK);
        when(logSender.getChannel()).thenReturn(NotificationChannel.LOG);

        ReminderDispatchService dispatchService = new ReminderDispatchService(
                reminderRepository,
                properties,
                clock,
                List.of(webhookSender, logSender)
        );

        ReminderEntity reminder = ReminderEntity.builder()
                .id(100L)
                .channel(NotificationChannel.WEBHOOK)
                .status(ReminderStatus.READY_TO_SEND)
                .attempts(0)
                .recipient("https://example.com/hook")
                .message("msg")
                .scheduledAt(Instant.parse("2026-04-06T09:00:00Z"))
                .build();

        when(reminderRepository.findById(100L))
                .thenReturn(Optional.of(reminder))
                .thenReturn(Optional.of(reminder))
                .thenReturn(Optional.of(reminder));
        when(reminderRepository.transitionStatus(eq(100L), anyCollection(), eq(ReminderStatus.PROCESSING), any()))
                .thenReturn(1);

        when(webhookSender.send(reminder)).thenReturn(SendResult.ok());
        when(reminderRepository.save(reminder)).thenReturn(reminder);

        dispatchService.dispatch(100L);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(reminder.getAttempts()).isEqualTo(1);
        verify(webhookSender).send(reminder);
        verify(logSender, never()).send(any());
    }
}
