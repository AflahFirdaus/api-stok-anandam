package com.stok.anandam.store.controller;

import com.stok.anandam.store.service.TrackingPublicService;
import com.stok.anandam.store.dto.UserTrackingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/tracking")
@RequiredArgsConstructor
public class TrackingPublicController {

    private final TrackingPublicService trackingPublicService;

    // Endpoint 1: via Token (QR / Tautan Langsung)
    @GetMapping("/{token}")
    public ResponseEntity<UserTrackingResponse> getTrackingData(@PathVariable UUID token) {
        UserTrackingResponse response = trackingPublicService.getTrackingDataByToken(token);
        return ResponseEntity.ok(response);
    }

    // Endpoint 2: via Nomor Telepon (Form Cari di Website)
    @GetMapping("/phone")
    public ResponseEntity<List<UserTrackingResponse>> getTrackingDataByPhone(@RequestParam String noTelepon) {
        List<UserTrackingResponse> response = trackingPublicService.getTrackingDataByPhone(noTelepon);
        return ResponseEntity.ok(response);
    }
}