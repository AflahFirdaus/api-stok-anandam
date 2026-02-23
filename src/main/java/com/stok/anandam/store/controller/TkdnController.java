package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Tkdn;
import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.TkdnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tkdn")
public class TkdnController {

    @Autowired
    private TkdnService tkdnService;

    /** GET /api/v1/tkdn – filter: isTkdn, kategori, search + spesifikasi (processor, ram, ssd, hdd, vga, layar, os). Semua filter AND, contains case-insensitive. */
    @GetMapping
    public ResponseEntity<WebResponse<List<Tkdn>>> getAllTkdn(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "nama") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam(name = "isTkdn", required = false) Boolean isTkdn,
            @RequestParam(name = "kategori", required = false) String kategori,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "processor", required = false) String processor,
            @RequestParam(name = "ram", required = false) String ram,
            @RequestParam(name = "ssd", required = false) String ssd,
            @RequestParam(name = "hdd", required = false) String hdd,
            @RequestParam(name = "vga", required = false) String vga,
            @RequestParam(name = "layar", required = false) String layar,
            @RequestParam(name = "os", required = false) String os
    ) {
        Page<Tkdn> data = tkdnService.getAllTkdn(page, size, sortBy, direction, isTkdn, kategori, search,
                processor, ram, ssd, hdd, vga, layar, os);
        if (data.getTotalPages() > 0 && page >= data.getTotalPages()) {
            data = tkdnService.getAllTkdn(0, size, sortBy, direction, isTkdn, kategori, search,
                    processor, ram, ssd, hdd, vga, layar, os);
        }

        PagingResponse paging = PagingResponse.builder()
                .currentPage(data.getNumber())
                .totalPage(data.getTotalPages())
                .size(size)
                .totalItem(data.getTotalElements())
                .build();

        return ResponseEntity.ok(WebResponse.<List<Tkdn>>builder()
                .status(200)
                .message("Success Fetch TKDN Data")
                .data(data.getContent())
                .paging(paging)
                .build());
    }

    /** GET /api/v1/tkdn/categories – daftar nilai unik kategori untuk dropdown (satu request, tanpa pagination). */
    @GetMapping("/categories")
    public ResponseEntity<WebResponse<List<String>>> getCategories() {
        List<String> categories = tkdnService.getCategories();
        return ResponseEntity.ok(WebResponse.<List<String>>builder()
                .status(200)
                .message("Success fetch TKDN categories")
                .data(categories)
                .paging(null)
                .build());
    }

    /** GET /api/v1/tkdn/filter-options – alternatif: { "kategori": ["A", "B", ...] } */
    @GetMapping("/filter-options")
    public ResponseEntity<WebResponse<Map<String, List<String>>>> getFilterOptions() {
        List<String> categories = tkdnService.getCategories();
        return ResponseEntity.ok(WebResponse.<Map<String, List<String>>>builder()
                .status(200)
                .message("Success fetch TKDN filter options")
                .data(Map.of("kategori", categories))
                .paging(null)
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
                .paging(null)
                .build());
    }
}