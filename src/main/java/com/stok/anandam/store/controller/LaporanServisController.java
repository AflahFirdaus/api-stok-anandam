package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.LaporanServisKeuanganResponse;
import com.stok.anandam.store.service.LaporanServisExportService;
import com.stok.anandam.store.service.LaporanServisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/laporan-servis") // Endpoint yang sangat deskriptif
@RequiredArgsConstructor
public class LaporanServisController {

    private final LaporanServisService laporanServisService;
    private final LaporanServisExportService exportService;

    @GetMapping("/keuangan")
    public ResponseEntity<LaporanServisKeuanganResponse> getKeuangan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(laporanServisService.getLaporanKeuanganServis(start, end));
    }

    @GetMapping("/keuangan/export")
    public ResponseEntity<String> exportKeuangan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        var data = laporanServisService.getLaporanKeuanganServis(start, end);
        String csvContent = exportService.generateCsv(data);

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"laporan-keuangan-servis.csv\"")
                .body(csvContent);
    }
}