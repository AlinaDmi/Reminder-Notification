package com.example.reminder.sender;

import com.example.reminder.entity.NotificationChannel;
import com.example.reminder.entity.ReminderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public SendResult send(ReminderEntity reminder) {
        log.info("EMAIL stub reminderId={} recipient={} subject={} message={}",
                reminder.getId(), reminder.getRecipient(), reminder.getSubject(), reminder.getMessage());
        return SendResult.ok();
    }
}
