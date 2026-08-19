package com.stok.anandam.store.controller;

import com.stok.anandam.store.annotation.LogActivity;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.MigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/migration")
public class MigrationController {

    @Autowired(required = false)
    private MigrationService migrationService;

    private ResponseEntity<WebResponse<String>> migrationDisabled() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(WebResponse.<String>builder()
                        .status(503)
                        .message(
                                "Migrasi tidak aktif. Set app.mysql.enabled=true di application.properties lalu restart aplikasi.")
                        .data(null)
                        .paging(null)
                        .build());
    }

    @PostMapping("/purchase")
    @LogActivity("MELAKUKAN MIGRASI DATA PURCHASE")
    public ResponseEntity<WebResponse<String>> startPurchaseMigration() {
        if (migrationService == null)
            return migrationDisabled();
        // migrationService.migratePurchaseData();
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Proses migrasi Purchase telah dipindah ke project api-migration.")
                .data("Moved")
                .paging(null)
                .build());
    }

    @PostMapping("/sales")
    @LogActivity("MELAKUKAN MIGRASI DATA SALES")
    public ResponseEntity<WebResponse<String>> startSalesMigration() {
        if (migrationService == null)
            return migrationDisabled();
        // migrationService.migrateSalesData();
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Migrasi Sales telah dipindah ke project api-migration.")
                .data("Moved")
                .paging(null)
                .build());
    }

    @PostMapping("/stock")
    @LogActivity("MELAKUKAN MIGRASI DATA STOK")
    public ResponseEntity<WebResponse<String>> startStockMigration() {
        if (migrationService == null)
            return migrationDisabled();
        // migrationService.migrateStockData();
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Migrasi Stok telah dipindah ke project api-migration.")
                .data("Moved")
                .paging(null)
                .build());
    }

    @PostMapping("/canvasing")
    @LogActivity("MELAKUKAN MIGRASI DATA CANVASSING")
    public ResponseEntity<WebResponse<String>> startCanvasingMigration() {
        if (migrationService == null)
            return migrationDisabled();
        migrationService.migrateCanvasingData();
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Migrasi Canvasing (dari CSV) berjalan di background...")
                .data("Processing...")
                .paging(null)
                .build());
    }

    @PostMapping("/tkdn")
    @LogActivity("MELAKUKAN MIGRASI DATA TKDN")
    public ResponseEntity<WebResponse<String>> startTkdnMigration() {
        if (migrationService == null)
            return migrationDisabled();
        migrationService.migrateTkdnData();
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Migrasi TKDN (dari CSV) berjalan di background...")
                .data("Processing...")
                .paging(null)
                .build());
    }

    @PostMapping("/sn")
    @LogActivity("MELAKUKAN MIGRASI DATA SERIAL NUMBER")
    public ResponseEntity<WebResponse<String>> startSnMigration() {
        if (migrationService == null)
            return migrationDisabled();
        // migrationService.migrateSnData(); // Pastikan method ini sudah dibuat di MigrationService
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Migrasi Serial Number telah dipindah ke project api-migration.")
                .data("Moved")
                .paging(null)
                .build());
    }

    @PostMapping({"/pelanggan-mybiz", "/pelanggan"})
    @LogActivity("MELAKUKAN MIGRASI DATA PELANGGAN (DISTRI / MYBIZ)")
    public ResponseEntity<WebResponse<String>> startPelangganMybizMigration() {
        if (migrationService == null)
            return migrationDisabled();
        migrationService.migratePelangganMybizData();
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Migrasi Pelanggan MyBiz berjalan di background...")
                .data("Processing...")
                .paging(null)
                .build());
    }


    @PostMapping("/distributor")
    @LogActivity("MELAKUKAN MIGRASI DATA DISTRIBUTOR & UPDATE is_ppn STOK")
    public ResponseEntity<WebResponse<String>> startDistributorMigration() {
        if (migrationService == null)
            return migrationDisabled();
        migrationService.migrateDistributorData();
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Migrasi Distributor (dari CSV) & update is_ppn stok berjalan di background...")
                .data("Processing...")
                .paging(null)
                .build());
    }

    @PostMapping("/pricelist")
    @LogActivity("MELAKUKAN MIGRASI DATA PRICELIST DARI SPREADSHEET")
    public ResponseEntity<WebResponse<String>> startPricelistMigration() {
        if (migrationService == null)
            return migrationDisabled();
        migrationService.syncStockPricelistFromSheet();
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Sinkronisasi Pricelist dari Spreadsheet sedang berjalan di background...")
                .data("Processing...")
                .paging(null)
                .build());
    }
}
// Re-trigger compilation