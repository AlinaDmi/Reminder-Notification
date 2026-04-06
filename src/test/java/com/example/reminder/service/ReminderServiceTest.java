package com.example.reminder.service;

import com.example.reminder.dto.CreateReminderRequest;
import com.example.reminder.dto.CreateReminderResponse;
import com.example.reminder.dto.ReminderResponse;
import com.example.reminder.entity.NotificationChannel;
import com.example.reminder.entity.ReminderEntity;
import com.example.reminder.entity.ReminderStatus;
import com.example.reminder.exception.InvalidReminderStateException;
import com.example.reminder.mapper.ReminderMapper;
import com.example.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private ReminderMapper reminderMapper;

    @Mock
    private ReminderDispatchService reminderDispatchService;

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-06T10:00:00Z"), ZoneOffset.UTC);
        reminderService = new ReminderService(reminderRepository, reminderMapper, reminderDispatchService, clock);
    }

    @Test
    void createShouldPersistReminderAndReturnCreatedStatus() {
        CreateReminderRequest request = new CreateReminderRequest();
        request.setMessage("Оплатить интернет");
        request.setChannel(NotificationChannel.LOG);
        request.setRecipient("local");
        request.setScheduledAt(Instant.parse("2026-04-10T18:00:00Z"));

        ReminderEntity mapped = ReminderEntity.builder().build();
        ReminderEntity saved = ReminderEntity.builder().id(1L).status(ReminderStatus.CREATED).build();
        CreateReminderResponse response = CreateReminderResponse.builder()
                .id(1L)
                .status(ReminderStatus.CREATED)
                .build();

        when(reminderMapper.toEntity(request)).thenReturn(mapped);
        when(reminderRepository.save(mapped)).thenReturn(saved);
        when(reminderMapper.toCreateResponse(saved)).thenReturn(response);

        CreateReminderResponse result = reminderService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ReminderStatus.CREATED);
        verify(reminderRepository).save(mapped);
    }

    @Test
    void retryShouldDispatchWhenReminderIsFailed() {
        ReminderEntity failedReminder = ReminderEntity.builder()
                .id(10L)
                .status(ReminderStatus.FAILED)
                .attempts(3)
                .build();

        ReminderResponse response = ReminderResponse.builder()
                .id(10L)
                .status(ReminderStatus.READY_TO_SEND)
                .attempts(3)
                .build();

        when(reminderRepository.findById(10L)).thenReturn(Optional.of(failedReminder));
        when(reminderRepository.saveAndFlush(failedReminder)).thenReturn(failedReminder);
        when(reminderMapper.toResponse(failedReminder)).thenReturn(response);

        ReminderResponse result = reminderService.retry(10L);

        assertThat(result.getStatus()).isEqualTo(ReminderStatus.READY_TO_SEND);
        verify(reminderDispatchService).dispatchAsync(10L);
    }

    @Test
    void retryShouldFailWhenReminderIsNotFailed() {
        ReminderEntity sentReminder = ReminderEntity.builder()
                .id(11L)
                .status(ReminderStatus.SENT)
                .build();

        when(reminderRepository.findById(11L)).thenReturn(Optional.of(sentReminder));

        assertThatThrownBy(() -> reminderService.retry(11L))
                .isInstanceOf(InvalidReminderStateException.class)
                .hasMessageContaining("FAILED reminders");
    }
}
