package com.example.reminder.sender;

import com.example.reminder.entity.NotificationChannel;
import com.example.reminder.entity.ReminderEntity;

public interface NotificationSender {

    NotificationChannel getChannel();

    SendResult send(ReminderEntity reminder);
}
