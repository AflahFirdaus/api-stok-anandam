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

    @Bean
    public CommandLineRunner addModelSeriLamaColumn(@org.springframework.beans.factory.annotation.Qualifier("pgJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("Checking if model_seri_lama column exists in transaksi_servis...");
                jdbcTemplate.execute("ALTER TABLE transaksi_servis ADD COLUMN IF NOT EXISTS model_seri_lama VARCHAR(100)");
                log.info("Column 'model_seri_lama' has been added (or already exists) in transaksi_servis table.");
            } catch (Exception e) {
                log.warn("Could not add column model_seri_lama: {}", e.getMessage());
            }
        };
    }
}
