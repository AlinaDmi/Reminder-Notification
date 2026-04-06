package com.example.reminder.controller;

import com.example.reminder.dto.CreateReminderRequest;
import com.example.reminder.dto.CreateReminderResponse;
import com.example.reminder.dto.ReminderResponse;
import com.example.reminder.entity.ReminderStatus;
import com.example.reminder.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateReminderResponse create(@Valid @RequestBody CreateReminderRequest request) {
        return reminderService.create(request);
    }

    @GetMapping("/{id}")
    public ReminderResponse getById(@PathVariable Long id) {
        return reminderService.getById(id);
    }

    @GetMapping
    public List<ReminderResponse> list(@RequestParam(required = false) ReminderStatus status) {
        return reminderService.list(status);
    }

    @PostMapping("/{id}/cancel")
    public ReminderResponse cancel(@PathVariable Long id) {
        return reminderService.cancel(id);
    }

    @PostMapping("/{id}/retry")
    public ReminderResponse retry(@PathVariable Long id) {
        return reminderService.retry(id);
    }
}
