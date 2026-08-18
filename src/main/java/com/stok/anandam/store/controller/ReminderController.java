package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @GetMapping
    public ResponseEntity<WebResponse<List<Map<String, Object>>>> getReminders() {
        List<Map<String, Object>> data = reminderService.getAllReminders();
        WebResponse<List<Map<String, Object>>> response = WebResponse.<List<Map<String, Object>>>builder()
                .status(HttpStatus.OK.value())
                .message("Success fetch reminders")
                .data(data)
                .paging(null)
                .build();
        return ResponseEntity.ok(response);
    }
}