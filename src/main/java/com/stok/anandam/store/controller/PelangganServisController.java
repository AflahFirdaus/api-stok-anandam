package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.PelangganServis;
import com.stok.anandam.store.service.PelangganServisService;
import com.stok.anandam.store.dto.CreatePelangganServisRequest;
import com.stok.anandam.store.dto.UpdatePelangganServisRequest;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.dto.PagingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pelanggan-servis")
@RequiredArgsConstructor
public class PelangganServisController {

    private final PelangganServisService pelangganServisService;

    // 1. CREATE API
    @PostMapping
    public ResponseEntity<PelangganServis> createPelanggan(@Valid @RequestBody CreatePelangganServisRequest request) {
        PelangganServis response = pelangganServisService.buatPelangganBaru(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. READ ALL API (PAGINATION & SEARCH) — konsisten dengan StockController
    @GetMapping
    public ResponseEntity<WebResponse<Page<PelangganServis>>> getAllPelanggan(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
            
        Page<PelangganServis> pelangganPage = pelangganServisService.getAllPelanggan(search, page, size);

        PagingResponse paging = PagingResponse.builder()
                .currentPage(page)
                .totalPage(pelangganPage.getTotalPages())
                .size(size)
                .totalItem(pelangganPage.getTotalElements())
                .build();

        return ResponseEntity.ok(WebResponse.<Page<PelangganServis>>builder()
                .status(200)
                .message("Success Fetch Pelanggan Servis Data")
                .data(pelangganPage)
                .paging(paging)
                .build());
    }

    // 3. READ BY ID API
    @GetMapping("/{id}")
    public ResponseEntity<PelangganServis> getPelangganById(@PathVariable UUID id) {
        PelangganServis response = pelangganServisService.getPelangganById(id);
        return ResponseEntity.ok(response);
    }

    // 4. UPDATE API
    @PutMapping("/{id}")
    public ResponseEntity<PelangganServis> updatePelanggan(
            @PathVariable UUID id, 
            @Valid @RequestBody UpdatePelangganServisRequest request) {
        PelangganServis response = pelangganServisService.updatePelanggan(id, request);
        return ResponseEntity.ok(response);
    }

    // 5. DELETE API
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePelanggan(@PathVariable UUID id) {
        pelangganServisService.deletePelanggan(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
    
}
