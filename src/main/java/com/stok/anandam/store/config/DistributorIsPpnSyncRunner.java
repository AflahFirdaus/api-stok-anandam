package com.stok.anandam.store.config;

import com.stok.anandam.store.service.MigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Saat aplikasi start, impor daftar distributor dari CSV lalu hitung ulang
 * is_ppn tiap item stok (cocokkan purchases.par_name -> distributor.tipe_pajak).
 *
 * Idempotent & aman dipanggil setiap boot: CSV adalah sumber kebenaran tabel
 * distributor, dan perhitungan is_ppn hanya memperbarui kolom is_ppn di stok.
 */
@Component
public class DistributorIsPpnSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DistributorIsPpnSyncRunner.class);

    private final MigrationService migrationService;

    public DistributorIsPpnSyncRunner(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String result = migrationService.migrateDistributorData().get();
            log.info("Distributor & is_ppn sync saat startup: {}", result);
        } catch (Exception e) {
            // Jangan sampai menghalangi startup; log & lanjut.
            log.warn("Gagal sinkronisasi distributor / is_ppn saat startup: {}", e.getMessage());
        }
    }
}