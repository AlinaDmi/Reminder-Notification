package com.example.reminder;

import com.example.reminder.config.ReminderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ReminderProperties.class)
public class ReminderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderApplication.class, args);
    }
}
