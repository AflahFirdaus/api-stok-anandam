package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Canvasing;
import com.stok.anandam.store.core.postgres.repository.CanvasingRepository;
import com.stok.anandam.store.dto.CanvasingOptionResponse;
import com.stok.anandam.store.dto.PagingResponse;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/canvasing")
public class CanvasingController {

    @Autowired
    private CanvasingRepository canvasingRepository;

    @GetMapping
    public ResponseEntity<WebResponse<List<Canvasing>>> getAllCanvasing(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "namaInstansi") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "kategori", required = false) String kategori,
            @RequestParam(name = "provinsi", required = false) String provinsi
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<Canvasing> result = canvasingRepository.search(search, kategori, provinsi, pageable);
        if (result.getTotalPages() > 0 && page >= result.getTotalPages()) {
            pageable = PageRequest.of(0, size, Sort.by(sortDirection, sortBy));
            result = canvasingRepository.search(search, kategori, provinsi, pageable);
            page = 0;
        }

        PagingResponse paging = PagingResponse.builder()
                .currentPage(page)
                .totalPage(result.getTotalPages())
                .size(size)
                .totalItem(result.getTotalElements())
                .build();

        return ResponseEntity.ok(WebResponse.<List<Canvasing>>builder()
                .status(200)
                .message("Success Fetch Canvasing Data")
                .data(result.getContent())
                .paging(paging)
                .build());
    }

    /** GET /api/v1/canvasing/options – ringan untuk dropdown/autocomplete: id + namaInstansi, dengan search & limit (default 50). */
    @GetMapping("/options")
    public ResponseEntity<WebResponse<List<CanvasingOptionResponse>>> getOptions(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 100), Sort.by(Sort.Direction.ASC, "namaInstansi"));
        Page<Canvasing> page = canvasingRepository.search(search, null, null, pageable);
        List<CanvasingOptionResponse> options = page.getContent().stream()
                .map(c -> CanvasingOptionResponse.builder()
                        .id(c.getId())
                        .namaInstansi(c.getNamaInstansi())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(WebResponse.<List<CanvasingOptionResponse>>builder()
                .status(200)
                .message("Success fetch canvasing options")
                .data(options)
                .paging(null)
                .build());
    }
}