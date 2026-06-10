package com.stok.anandam.store.controller;

import com.stok.anandam.store.service.WaLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifikasi")
@RequiredArgsConstructor
public class WaNotifikasiController {

    private final WaLinkService waLinkService;

    // Endpoint: GET /api/v1/notifikasi/wa-link/{id}?tipePesan=SELESAI
    @GetMapping("/wa-link/{transaksiId}")
    public ResponseEntity<Map<String, String>> getWaLink(
            @PathVariable UUID transaksiId,
            @RequestParam(defaultValue = "SELESAI") String tipePesan) {
            
        String link = waLinkService.generateWaLink(transaksiId, tipePesan);
        
        // Membungkus link dalam bentuk JSON: { "waLink": "https://wa.me/..." }
        return ResponseEntity.ok(Collections.singletonMap("waLink", link));
    }
}