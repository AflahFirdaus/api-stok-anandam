package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Tkdn;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.TkdnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tkdn")
public class TkdnController {

    @Autowired
    private TkdnService tkdnService;

    // GET /api/tkdn (ALL)
    // GET /api/tkdn?isTkdn=true (Hanya yang TKDN)
    // GET /api/tkdn?search=Laptop (Cari nama/merek)
    @GetMapping
    public ResponseEntity<WebResponse<Page<Tkdn>>> getAllTkdn(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "nama") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam(name = "isTkdn", required = false) Boolean isTkdn,
            @RequestParam(name = "kategori", required = false) String kategori,
            @RequestParam(name = "search", required = false) String search
    ) {
        // Masukkan kategori ke service
        Page<Tkdn> data = tkdnService.getAllTkdn(page, size, sortBy, direction, isTkdn, kategori, search);

        return ResponseEntity.ok(WebResponse.<Page<Tkdn>>builder()
                .status(200)
                .message("Success Fetch TKDN Data")
                .data(data)
                .build());
    }

    // GET /api/tkdn/{id} (Detail TKDN)
    @GetMapping("/{id}")
    public ResponseEntity<WebResponse<Tkdn>> getTkdnDetail(@PathVariable Integer id) {
        Tkdn tkdn = tkdnService.getTkdnById(id);

        return ResponseEntity.ok(WebResponse.<Tkdn>builder()
                .status(200)
                .message("Success Fetch Detail TKDN")
                .data(tkdn)
                .build());
    }
}