package com.stok.anandam.store.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalTime;

@Configuration
@EnableScheduling
@Component
public class MigrationScheduler {

    @Autowired
    private MigrationService migrationService;

    // Tetap jalan setiap 60 detik (1 Menit)
    @Scheduled(fixedDelay = 60000)
    public void scheduleMigration() {
        LocalTime now = LocalTime.now();

        // Istirahat malam hari (21:15 s/d 07:55 pagi)
        if (now.isAfter(LocalTime.of(21, 15)) || now.isBefore(LocalTime.of(7, 55))) {
            return;
        }

        System.out.println("Menjalankan migrasi data...");
        migrationService.checkAndTriggerMigration();
    }
}