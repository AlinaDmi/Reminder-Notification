package com.example.reminder.scheduler;

import com.example.reminder.entity.ReminderEntity;
import com.example.reminder.entity.ReminderStatus;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.service.ReminderDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderRepository reminderRepository;
    private final ReminderDispatchService reminderDispatchService;
    private final Clock clock;

    @Transactional
    @Scheduled(fixedDelayString = "${reminder.scheduler-delay-ms}")
    public void scheduleDueReminders() {
        Instant now = clock.instant();
        List<ReminderEntity> due = reminderRepository.findDueForDispatch(
                List.of(ReminderStatus.CREATED, ReminderStatus.RETRY_PENDING),
                now
        );

        for (ReminderEntity reminder : due) {
            int moved = reminderRepository.transitionStatus(
                    reminder.getId(),
                    List.of(reminder.getStatus()),
                    ReminderStatus.READY_TO_SEND,
                    now
            );
            if (moved == 1) {
                log.info("Reminder moved to READY_TO_SEND reminderId={} previousStatus={}",
                        reminder.getId(), reminder.getStatus());
                reminderDispatchService.dispatchAsync(reminder.getId());
            }
        }
    }
}
