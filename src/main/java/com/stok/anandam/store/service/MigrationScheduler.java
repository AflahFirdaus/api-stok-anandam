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
@ConditionalOnProperty(name = "app.mysql.enabled", havingValue = "true")
public class MigrationScheduler {

    @Autowired
    private MigrationService migrationService;

    // Tetap jalan setiap 30 detik
    @Scheduled(fixedDelay = 30000)
    public void scheduleMigration() {
        LocalTime now = LocalTime.now();
        System.out.println("Scheduler mengecek waktu: " + now); // Tambahkan ini sementara

        // Istirahat malam hari (21:15 s/d 07:55 pagi)
        if (now.isAfter(LocalTime.of(21, 15)) || now.isBefore(LocalTime.of(7, 55))) {
            System.out.println("Masih dalam jam istirahat malam, skip...");
            return;
        }

        System.out.println("Menjalankan migrasi data...");
        migrationService.checkAndTriggerMigration();
    }
}