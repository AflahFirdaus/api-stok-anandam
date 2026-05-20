package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Announcement;
import com.stok.anandam.store.core.postgres.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @GetMapping
    public ResponseEntity<?> getAnnouncements() {
        List<Announcement> announcements = announcementRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", announcements);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createAnnouncement(@RequestBody Announcement announcement) {
        Announcement saved = announcementRepository.save(announcement);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnnouncement(@PathVariable Integer id, @RequestBody Announcement announcement) {
        return announcementRepository.findById(id).map(existing -> {
            existing.setTitle(announcement.getTitle());
            existing.setSubtitle(announcement.getSubtitle());
            existing.setStartDate(announcement.getStartDate());
            existing.setExpiredDate(announcement.getExpiredDate());
            Announcement updated = announcementRepository.save(existing);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Integer id) {
        if (announcementRepository.existsById(id)) {
            announcementRepository.deleteById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Announcement deleted successfully");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }
}
