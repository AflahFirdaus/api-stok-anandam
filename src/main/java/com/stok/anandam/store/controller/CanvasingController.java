package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Canvasing;
import com.stok.anandam.store.core.postgres.repository.CanvasingRepository;
import com.stok.anandam.store.dto.WebResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@RestController
@RequestMapping("/api/v1/canvasing")
public class CanvasingController {

    @Autowired
    private CanvasingRepository canvasingRepository;

    @GetMapping
    public ResponseEntity<WebResponse<Page<Canvasing>>> getAllCanvasing(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "namaInstansi") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam(name = "search", required = false) String search // <--- Tambah ini
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        // Panggil Repository Search
        Page<Canvasing> result = canvasingRepository.search(search, pageable);

        return ResponseEntity.ok(WebResponse.<Page<Canvasing>>builder()
                .status(200)
                .message("Success Fetch Canvasing Data")
                .data(result)
                .build());
    }
}