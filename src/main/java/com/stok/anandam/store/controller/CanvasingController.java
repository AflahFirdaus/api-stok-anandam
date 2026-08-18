package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.PelangganItem;
import com.stok.anandam.store.dto.PelangganOption;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.CanvasingPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Master Pelanggan untuk fitur Canvasing. Data diambil dari tabel Pelanggan
 * di database Portal (eksternal).
 */
@RestController
@RequestMapping({"/api/v1/canvasings", "/api/v1/canvasing"})
public class CanvasingController {

    @Autowired
    private CanvasingPortalService portalService;

    @GetMapping
    public ResponseEntity<WebResponse<List<PelangganItem>>> getAllCanvasing(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "namaInstansi") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "kategori", required = false) String kategori,
            @RequestParam(name = "provinsi", required = false) String provinsi
    ) {
        Page<PelangganItem> result = portalService.getAllPelanggan(
                page, size, sortBy, direction, search, kategori, provinsi);

        PagingResponse paging = PagingResponse.builder()
                .currentPage(result.getNumber())
                .totalPage(result.getTotalPages())
                .size(size)
                .totalItem(result.getTotalElements())
                .build();

        return ResponseEntity.ok(WebResponse.<List<PelangganItem>>builder()
                .status(200)
                .message("Success Fetch Canvasing Data")
                .data(result.getContent())
                .paging(paging)
                .build());
    }

    /** GET /api/v1/canvasing/options – ringan untuk dropdown/autocomplete: id + namaInstansi, dengan search & limit (default 50). */
    @GetMapping("/options")
    public ResponseEntity<WebResponse<List<PelangganOption>>> getOptions(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        List<PelangganOption> options = portalService.getPelangganOptions(search, limit);
        return ResponseEntity.ok(WebResponse.<List<PelangganOption>>builder()
                .status(200)
                .message("Success fetch canvasing options")
                .data(options)
                .paging(null)
                .build());
    }
}