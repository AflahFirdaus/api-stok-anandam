package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.TransaksiServis;
import com.stok.anandam.store.core.postgres.repository.TransaksiServisRepository;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import com.stok.anandam.store.service.NotaPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nota")
@RequiredArgsConstructor
public class NotaController {

    private final NotaPdfService notaPdfService;
    private final TransaksiServisRepository transaksiRepository;

    @Transactional(readOnly = true)
    @GetMapping("/download/{transaksiId}")
    public ResponseEntity<byte[]> downloadNota(@PathVariable UUID transaksiId) {
    // 1. Ambil data transaksi
    TransaksiServis transaksi = transaksiRepository.findById(transaksiId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaksi tidak ditemukan"));

    // 2. Ambil informasi user yang sedang login (via Spring Security)
    String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
    boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    // 3. Security Guardrail: Cek apakah user adalah admin atau pemilik nota
    // (Misal: transaksi.getPelanggan().getNoTelepon() atau field identitas lainnya)
    boolean isOwner = transaksi.getPelanggan().getNoTelepon() != null && 
                      transaksi.getPelanggan().getNoTelepon().equals(currentUser);

    if (!isAdmin && !isOwner) {
        throw new AccessDeniedException("Anda tidak memiliki akses ke nota ini.");
    }

    // 4. Jika lolos, kirim file
    byte[] pdfBytes = notaPdfService.generatePdf(transaksi);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("Nota-" + transaksiId + ".pdf").build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

}
