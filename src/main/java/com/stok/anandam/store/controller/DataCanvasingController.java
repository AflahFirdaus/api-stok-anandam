package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.DataCanvasing;
import com.stok.anandam.store.dto.DataCanvasingRequest;
import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.DataCanvasingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/data-canvasing")
public class DataCanvasingController {

    @Autowired
    private DataCanvasingService service;

    // === 1. TAMBAH DATA ===
    @PostMapping
    public ResponseEntity<WebResponse<DataCanvasing>> create(@Valid @RequestBody DataCanvasingRequest request) {
        DataCanvasing result = service.create(request);
        return ResponseEntity.ok(WebResponse.<DataCanvasing>builder()
                .status(200)
                .message("Berhasil Menambahkan Data Kunjungan")
                .data(result)
                .paging(null)
                .build());
    }

    // === 2. GET ALL (PAGINATION) ===
    @GetMapping
    public ResponseEntity<WebResponse<List<DataCanvasing>>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "tanggal") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            // Tambahan Filter
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "canvasingId", required = false) Long canvasingId,
            @RequestParam(name = "canvasVisit", required = false) String canvasVisit
    ) {
        Page<DataCanvasing> result = service.getAll(page, size, sortBy, direction, startDate, endDate, search, canvasingId, canvasVisit);

        PagingResponse paging = PagingResponse.builder()
                .currentPage(result.getNumber())
                .totalPage(result.getTotalPages())
                .size(size)
                .totalItem(result.getTotalElements())
                .build();

        return ResponseEntity.ok(WebResponse.<List<DataCanvasing>>builder()
                .status(200)
                .message("Success Fetch Data Canvasing")
                .data(result.getContent())
                .paging(paging)
                .build());
        }
}