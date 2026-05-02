package com.stok.anandam.store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stok.anandam.store.core.postgres.model.AppUpdate;
import com.stok.anandam.store.core.postgres.repository.AppUpdateRepository;
import com.stok.anandam.store.dto.AppUpdatePublishRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class AppUpdateController {

    @Autowired
    private AppUpdateRepository appUpdateRepository;

    // Masukkan rahasia kunci ini di file application.properties
    // internal.api.key=KUNCI_RAHASIA_ANDA_YANG_SAMA_DENGAN_DI_GITHUB
    @Value("${internal.api.key:SECRET_KEY_DEFAULT}")
    private String internalApiKey;

    // 1. ENDPOINT PUBLIK (Diakses oleh HP untuk cek update)
    @GetMapping("/api/v1/public/app/latest")
    public ResponseEntity<?> getLatestUpdate() {
        Optional<AppUpdate> latest = appUpdateRepository.findFirstByOrderByVersionCodeDesc();
        
        Map<String, Object> response = new HashMap<>();
        if (latest.isPresent()) {
            response.put("status", "success");
            response.put("data", latest.get());
        } else {
            response.put("status", "success");
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    // 2. ENDPOINT INTERNAL (Diakses oleh GitHub Actions saat publish)
    @PostMapping("/api/v1/internal/app/publish")
    public ResponseEntity<?> publishUpdate(
            @RequestHeader("X-Internal-Key") String authKey,
            @RequestBody AppUpdatePublishRequest request) {
        
        // Cek keamanan (Apakah GitHub mengirim kunci yang benar?)
        if (!internalApiKey.equals(authKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        AppUpdate update = new AppUpdate();
        update.setVersionName(request.getVersionName());
        update.setVersionCode(request.getVersionCode());
        update.setDownloadUrl(request.getDownloadUrl());
        update.setReleaseNotes(request.getReleaseNotes());
        update.setIsForceUpdate(request.getIsForceUpdate() != null ? request.getIsForceUpdate() : false);
        update.setCreatedAt(LocalDateTime.now());

        appUpdateRepository.save(update);

        return ResponseEntity.ok("Version published successfully!");
    }
}
