package com.stok.anandam.store.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DatabaseMigrationConfig {

    @Bean
    public CommandLineRunner fixDatabaseConstraints(@org.springframework.beans.factory.annotation.Qualifier("pgJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("Checking database constraints for penjadwalan_konfirmasi...");
                // Hapus constraint lama agar enum baru (DIBATALKAN) bisa masuk
                jdbcTemplate.execute("ALTER TABLE penjadwalan_konfirmasi DROP CONSTRAINT IF EXISTS penjadwalan_konfirmasi_status_jadwal_check");
                log.info("Constraint 'penjadwalan_konfirmasi_status_jadwal_check' has been dropped successfully.");
            } catch (Exception e) {
                log.warn("Could not drop constraint (it might not exist): {}", e.getMessage());
            }
        };
    }
}
