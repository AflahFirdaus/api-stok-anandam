package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.ReminderItem;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private static final Logger log = LoggerFactory.getLogger(ReminderController.class);

    @Autowired
    private ReminderService reminderService;

    @GetMapping
    public ResponseEntity<WebResponse<List<ReminderItem>>> getReminders() {
        try {
            List<ReminderItem> data = reminderService.getAllReminders();
            WebResponse<List<ReminderItem>> response = WebResponse.<List<ReminderItem>>builder()
                    .status(HttpStatus.OK.value())
                    .message("Success fetch reminders")
                    .data(data)
                    .paging(null)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Gagal memuat reminder dari database portal: {}", e.getMessage(), e);
            WebResponse<List<ReminderItem>> error = WebResponse.<List<ReminderItem>>builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Gagal memuat reminder: " + e.getMessage())
                    .data(null)
                    .paging(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}