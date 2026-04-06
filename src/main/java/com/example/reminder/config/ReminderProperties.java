package com.example.reminder.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "reminder")
public class ReminderProperties {

    @Min(1)
    private int maxAttempts = 3;

    @Min(1)
    private long retryDelaySeconds = 60;

    @Min(100)
    private long schedulerDelayMs = 5000;

    @Min(100)
    private long webhookTimeoutMs = 3000;

    @Valid
    private AsyncProperties async = new AsyncProperties();

    @Data
    public static class AsyncProperties {

        @Min(1)
        private int corePoolSize = 4;

        @Min(1)
        private int maxPoolSize = 8;

        @Min(1)
        private int queueCapacity = 100;

        private String threadNamePrefix = "reminder-dispatch-";
    }
}
