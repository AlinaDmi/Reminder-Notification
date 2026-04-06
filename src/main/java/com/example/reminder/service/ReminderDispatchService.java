package com.example.reminder.service;

import com.example.reminder.config.ReminderProperties;
import com.example.reminder.entity.NotificationChannel;
import com.example.reminder.entity.ReminderEntity;
import com.example.reminder.entity.ReminderStatus;
import com.example.reminder.exception.SenderNotConfiguredException;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.sender.NotificationSender;
import com.example.reminder.sender.SendResult;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReminderDispatchService {

    private final ReminderRepository reminderRepository;
    private final ReminderProperties reminderProperties;
    private final Clock clock;
    private final Map<NotificationChannel, NotificationSender> senderByChannel;

    public ReminderDispatchService(
            ReminderRepository reminderRepository,
            ReminderProperties reminderProperties,
            Clock clock,
            List<NotificationSender> senders
    ) {
        this.reminderRepository = reminderRepository;
        this.reminderProperties = reminderProperties;
        this.clock = clock;
        this.senderByChannel = buildSenderMap(senders);
    }

    @Async("notificationTaskExecutor")
    public void dispatchAsync(Long reminderId) {
        dispatch(reminderId);
    }

    public void dispatch(Long reminderId) {
        ReminderEntity current = reminderRepository.findById(reminderId).orElse(null);
        if (current == null) {
            log.warn("Dispatch skipped, reminder not found reminderId={}", reminderId);
            return;
        }
        if (current.getStatus() == ReminderStatus.SENT || current.getStatus() == ReminderStatus.CANCELLED) {
            return;
        }

        int moved = reminderRepository.transitionStatus(
                reminderId,
                List.of(ReminderStatus.READY_TO_SEND, ReminderStatus.RETRY_PENDING),
                ReminderStatus.PROCESSING,
                clock.instant()
        );
        if (moved == 0) {
            log.debug("Dispatch skipped, reminder is not claimable reminderId={}", reminderId);
            return;
        }

        ReminderEntity reminder = reminderRepository.findById(reminderId).orElse(null);
        if (reminder == null) {
            return;
        }

        NotificationSender sender = senderByChannel.get(reminder.getChannel());
        if (sender == null) {
            saveFailedResult(reminder, "Sender is not configured for channel " + reminder.getChannel());
            throw new SenderNotConfiguredException(reminder.getChannel());
        }

        SendResult sendResult;
        try {
            log.info("Dispatch started reminderId={} channel={}", reminder.getId(), reminder.getChannel());
            sendResult = sender.send(reminder);
        } catch (Exception ex) {
            sendResult = SendResult.fail(ex.getMessage());
        }

        if (sendResult.success()) {
            saveSuccessResult(reminderId);
        } else {
            saveFailedResult(reminderId, sendResult.errorMessage());
        }
    }

    private void saveSuccessResult(Long reminderId) {
        ReminderEntity reminder = reminderRepository.findById(reminderId).orElse(null);
        if (reminder == null) {
            return;
        }
        reminder.setAttempts(reminder.getAttempts() + 1);
        reminder.setStatus(ReminderStatus.SENT);
        reminder.setLastError(null);
        reminder.setNextRetryAt(null);
        reminderRepository.save(reminder);
        log.info("Dispatch success reminderId={} attempts={}", reminder.getId(), reminder.getAttempts());
    }

    private void saveFailedResult(ReminderEntity reminder, String message) {
        saveFailedResult(reminder.getId(), message);
    }

    private void saveFailedResult(Long reminderId, String message) {
        ReminderEntity reminder = reminderRepository.findById(reminderId).orElse(null);
        if (reminder == null) {
            return;
        }
        reminder.setAttempts(reminder.getAttempts() + 1);
        reminder.setLastError(trimError(message));

        if (reminder.getAttempts() < reminderProperties.getMaxAttempts()) {
            reminder.setStatus(ReminderStatus.RETRY_PENDING);
            reminder.setNextRetryAt(Instant.now(clock).plusSeconds(reminderProperties.getRetryDelaySeconds()));
            log.warn("Dispatch failed reminderId={} attempts={} nextRetryAt={}",
                    reminder.getId(), reminder.getAttempts(), reminder.getNextRetryAt());
        } else {
            reminder.setStatus(ReminderStatus.FAILED);
            reminder.setNextRetryAt(null);
            log.warn("Dispatch permanently failed reminderId={} attempts={}", reminder.getId(), reminder.getAttempts());
        }

        reminderRepository.save(reminder);
    }

    private Map<NotificationChannel, NotificationSender> buildSenderMap(List<NotificationSender> senders) {
        EnumMap<NotificationChannel, NotificationSender> map = new EnumMap<>(NotificationChannel.class);
        for (NotificationSender sender : senders) {
            map.put(sender.getChannel(), sender);
        }
        return Map.copyOf(map);
    }

    private String trimError(String message) {
        if (message == null) {
            return "Unknown error";
        }
        String sanitized = message.trim();
        if (sanitized.length() <= 1900) {
            return sanitized;
        }
        return sanitized.substring(0, 1900);
    }
}
