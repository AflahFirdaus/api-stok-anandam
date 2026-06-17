package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.SilentPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/printer")
@RequiredArgsConstructor
public class PrinterController {

    private final SilentPrintService silentPrintService;

    /**
     * Endpoint utama untuk menerima PDF dari Flutter dan langsung mencetaknya.
     */
    @PostMapping("/print")
    public ResponseEntity<WebResponse<String>> printDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "printerName", required = false) String printerName) {
        
        // Memanggil service untuk mengeksekusi print
        silentPrintService.printPdfSilent(file, printerName);
        
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Berhasil mengirim dokumen ke antrean printer.")
                .data(null) 
                .paging(null)
                .build());
    }

    /**
     * Endpoint tambahan (opsional) untuk mengecek printer apa saja
     * yang terdeteksi oleh server (sangat berguna untuk debugging).
     */
    @GetMapping("/available")
    public ResponseEntity<WebResponse<String>> getAvailablePrinters() {
        String availablePrinters = silentPrintService.getAvailablePrinters();
        
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Success Fetch Available Printers")
                .data(availablePrinters)
                .paging(null)
                .build());
    }
}