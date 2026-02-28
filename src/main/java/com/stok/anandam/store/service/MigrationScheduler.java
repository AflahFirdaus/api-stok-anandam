package com.stok.anandam.store.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Configuration
@EnableScheduling
@Component
public class MigrationScheduler {

    @Autowired
    private MigrationService migrationService;

    // Run every 30 seconds
    @Scheduled(fixedDelay = 30000)
    public void scheduleMigration() {
        migrationService.checkAndTriggerMigration();
    }
}
