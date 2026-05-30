package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.IjinImport;
import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.IjinImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ijin-import")
public class IjinImportController {

    @Autowired
    private IjinImportService ijinImportService;

    // GET /api/v1/ijin-import?page=0&size=10&search=printer
    @GetMapping
    public ResponseEntity<WebResponse<List<IjinImport>>> getAllIjinImport(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "no") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam(name = "search", required = false) String search) {

        Page<IjinImport> pageResult = ijinImportService.getIjinImportData(page, size, sortBy, direction, search);

        // Reset ke page 0 jika data halaman yang diminta kosong padahal total item ada
        if (pageResult.getContent().isEmpty() && pageResult.getTotalElements() > 0 && page > 0) {
            pageResult = ijinImportService.getIjinImportData(0, size, sortBy, direction, search);
            page = 0;
        }

        // Bangun PagingResponse sesuai format proyek Anda
        PagingResponse paging = PagingResponse.builder()
                .currentPage(page)
                .totalPage(pageResult.getTotalPages())
                .size(size)
                .totalItem(pageResult.getTotalElements())
                .build();

        // Kirim WebResponse baku
        return ResponseEntity.ok(WebResponse.<List<IjinImport>>builder()
                .status(200)
                .message("Success Fetch Ijin Import Data")
                .data(pageResult.getContent())
                .paging(paging)
                .build());
    }
}
