package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.CanvasVisitRecord;
import com.stok.anandam.store.dto.DataCanvasingRequest;
import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.CanvasingPortalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Fitur Catat Canvas: kunjungan disimpan & dibaca dari tabel Canvas
 * di database Portal (eksternal).
 */
@RestController
@RequestMapping("/api/v1/data-canvasing")
public class DataCanvasingController {

    @Autowired
    private CanvasingPortalService portalService;

    // === 1. TAMBAH DATA (INSERT ke Canvas eksternal) ===
    @PostMapping
    public ResponseEntity<WebResponse<String>> create(
            @Valid @RequestBody DataCanvasingRequest request) {
        String id = portalService.createCanvas(
                request.getPelangganId(),
                request.getTanggal(),
                request.getCanvasVisit(),
                request.getKeterangan(),
                request.getCatatan());
        return ResponseEntity.ok(WebResponse.<String>builder()
                .status(200)
                .message("Berhasil Menambahkan Data Kunjungan")
                .data(id)
                .paging(null)
                .build());
    }

    // === 2. GET ALL RIWAYAT (SELECT dari Canvas eksternal) ===
    @GetMapping
    public ResponseEntity<WebResponse<List<CanvasVisitRecord>>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "tanggal") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "canvasingId", required = false) String canvasingId,
            @RequestParam(name = "canvasVisit", required = false) String canvasVisit
    ) {
        Page<CanvasVisitRecord> result = portalService.getAllCanvas(
                page, size, startDate, endDate, search, canvasingId, canvasVisit);

        PagingResponse paging = PagingResponse.builder()
                .currentPage(result.getNumber())
                .totalPage(result.getTotalPages())
                .size(size)
                .totalItem(result.getTotalElements())
                .build();

        return ResponseEntity.ok(WebResponse.<List<CanvasVisitRecord>>builder()
                .status(200)
                .message("Success Fetch Data Canvasing")
                .data(result.getContent())
                .paging(paging)
                .build());
    }
}