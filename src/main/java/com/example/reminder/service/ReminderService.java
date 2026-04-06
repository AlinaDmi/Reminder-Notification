package com.example.reminder.service;

import com.example.reminder.dto.CreateReminderRequest;
import com.example.reminder.dto.CreateReminderResponse;
import com.example.reminder.dto.ReminderResponse;
import com.example.reminder.entity.ReminderEntity;
import com.example.reminder.entity.ReminderStatus;
import com.example.reminder.exception.InvalidReminderStateException;
import com.example.reminder.exception.ReminderNotFoundException;
import com.example.reminder.mapper.ReminderMapper;
import com.example.reminder.repository.ReminderRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ReminderMapper reminderMapper;
    private final ReminderDispatchService reminderDispatchService;
    private final Clock clock;

    @Transactional
    public CreateReminderResponse create(CreateReminderRequest request) {
        ReminderEntity entity = reminderMapper.toEntity(request);
        ReminderEntity saved = reminderRepository.save(entity);
        log.info("Reminder created reminderId={} scheduledAt={} channel={}",
                saved.getId(), saved.getScheduledAt(), saved.getChannel());
        return reminderMapper.toCreateResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReminderResponse getById(Long id) {
        ReminderEntity reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ReminderNotFoundException(id));
        return reminderMapper.toResponse(reminder);
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> list(ReminderStatus status) {
        List<ReminderEntity> reminders = status == null
                ? reminderRepository.findAll()
                : reminderRepository.findAllByStatusOrderByScheduledAtAsc(status);
        return reminders.stream()
                .map(reminderMapper::toResponse)
                .toList();
    }

    @Transactional
    public ReminderResponse cancel(Long id) {
        ReminderEntity reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ReminderNotFoundException(id));
        if (reminder.getStatus() == ReminderStatus.SENT ||
                reminder.getStatus() == ReminderStatus.CANCELLED ||
                reminder.getStatus() == ReminderStatus.PROCESSING) {
            throw new InvalidReminderStateException("Reminder cannot be cancelled from state " + reminder.getStatus());
        }
        reminder.setStatus(ReminderStatus.CANCELLED);
        reminder.setLastError(null);
        reminder.setNextRetryAt(null);
        ReminderEntity saved = reminderRepository.save(reminder);
        log.info("Reminder cancelled reminderId={}", saved.getId());
        return reminderMapper.toResponse(saved);
    }

    @Transactional
    public ReminderResponse retry(Long id) {
        ReminderEntity reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ReminderNotFoundException(id));
        if (reminder.getStatus() != ReminderStatus.FAILED) {
            throw new InvalidReminderStateException("Manual retry is available only for FAILED reminders");
        }

        reminder.setStatus(ReminderStatus.READY_TO_SEND);
        reminder.setLastError(null);
        reminder.setNextRetryAt(clock.instant());
        ReminderEntity saved = reminderRepository.saveAndFlush(reminder);
        reminderDispatchService.dispatchAsync(saved.getId());

        log.info("Manual retry requested reminderId={} attempts={}", saved.getId(), saved.getAttempts());
        return reminderMapper.toResponse(saved);
    }
}
