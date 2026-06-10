package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.KlaimDistributor;
import com.stok.anandam.store.core.postgres.repository.KlaimDistributorRepository;
import com.stok.anandam.store.dto.KlaimDistributorResponse;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/klaim-distributor")
@RequiredArgsConstructor
public class KlaimDistributorController {

    private final KlaimDistributorRepository klaimRepository;

    @GetMapping("/by-transaksi/{transaksiId}")
    public ResponseEntity<KlaimDistributorResponse> getKlaimByTransaksi(@PathVariable UUID transaksiId) {
        KlaimDistributor klaim = klaimRepository.findByTransaksiId(transaksiId)
                .orElseThrow(() -> new ResourceNotFoundException("Klaim distribusi tidak ditemukan untuk transaksi ini"));

        return ResponseEntity.ok(KlaimDistributorResponse.fromEntity(klaim));
    }
}