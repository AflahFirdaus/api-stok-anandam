package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.KategoriPelanggan;
import com.stok.anandam.store.core.postgres.model.PelangganServis;
import com.stok.anandam.store.core.postgres.repository.PelangganServisRepository;
import com.stok.anandam.store.dto.CreatePelangganServisRequest;
import com.stok.anandam.store.dto.UpdatePelangganServisRequest;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PelangganServisService {

    private final PelangganServisRepository pelangganServisRepository;

    // --- CREATE ---
    @Transactional
    public PelangganServis buatPelangganBaru(CreatePelangganServisRequest request) {
        
        // 1. Validasi Nomor Telepon Unik (Sesuai catatan di form lama)
        if (request.getNoTelepon() != null && pelangganServisRepository.existsByNoTelepon(request.getNoTelepon())) {
            throw new DataIntegrityViolationException("Nomor telepon " + request.getNoTelepon() + " sudah terdaftar untuk pelanggan lain.");
        }

        // 2. Sanitasi & Auto-Format Nomor WhatsApp
        String formattedWa = formatToWhatsApp(
                request.getNoWhatsapp() != null && !request.getNoWhatsapp().trim().isEmpty() 
                ? request.getNoWhatsapp() 
                : request.getNoTelepon() // Fallback: Jika WA kosong, otomatis gunakan no telepon
        );

        PelangganServis pelangganBaru = PelangganServis.builder()
                .namaPelanggan(request.getNamaPelanggan())
                .kategori(request.getKategori() != null ? request.getKategori() : KategoriPelanggan.User)
                .noTelepon(request.getNoTelepon())
                .noWhatsapp(formattedWa)
                .alamat(request.getAlamat())
                .build();

        return pelangganServisRepository.save(pelangganBaru);
    }

    // --- HELPER: Auto-Format WhatsApp ---
    private String formatToWhatsApp(String rawNumber) {
        if (rawNumber == null || rawNumber.trim().isEmpty()) {
            return null;
        }

        // Hapus semua spasi, strip, dan karakter non-angka kecuali '+'
        String cleaned = rawNumber.replaceAll("[^0-9+]", "");

        // Ubah awalan 0 menjadi 62 (Kode Negara Indonesia)
        if (cleaned.startsWith("0")) {
            return "62" + cleaned.substring(1);
        }
        // Hapus tanda + jika ada (contoh: +62 -> 62)
        if (cleaned.startsWith("+")) {
            return cleaned.substring(1);
        }
        
        return cleaned; // Kembalikan apa adanya jika sudah diawali 62 atau kode negara lain
    }
    // --- READ (ALL) ---
    @Transactional(readOnly = true)
    public List<PelangganServis> getAllPelanggan() {
        return pelangganServisRepository.findAll();
    }

    // --- READ (BY ID) ---
    @Transactional(readOnly = true)
    public PelangganServis getPelangganById(UUID id) {
        return pelangganServisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data pelanggan servis tidak ditemukan dengan ID: " + id));
    }

    // --- UPDATE ---
    @Transactional
    public PelangganServis updatePelanggan(UUID id, UpdatePelangganServisRequest request) {
        PelangganServis pelanggan = getPelangganById(id); // Gunakan method read di atas agar validasi terpusat

        // 1. Validasi nomor telepon baru jika berubah
        if (request.getNoTelepon() != null && !request.getNoTelepon().equals(pelanggan.getNoTelepon())) {
            if (pelangganServisRepository.existsByNoTelepon(request.getNoTelepon())) {
                throw new DataIntegrityViolationException("Nomor telepon " + request.getNoTelepon() + " sudah digunakan pelanggan lain.");
            }
        }

        pelanggan.setNamaPelanggan(request.getNamaPelanggan());
        if (request.getKategori() != null) {
            pelanggan.setKategori(request.getKategori());
        }
        if (request.getNoTelepon() != null) {
            pelanggan.setNoTelepon(request.getNoTelepon());
        }
        
        // 2. Sanitasi nomor WhatsApp
        String formattedWa = formatToWhatsApp(
                request.getNoWhatsapp() != null && !request.getNoWhatsapp().trim().isEmpty() 
                ? request.getNoWhatsapp() 
                : request.getNoTelepon() // Fallback: Jika WA kosong, otomatis gunakan no telepon
        );
        pelanggan.setNoWhatsapp(formattedWa);
        
        pelanggan.setAlamat(request.getAlamat());

        return pelangganServisRepository.save(pelanggan);
    }

    // --- DELETE ---
    @Transactional
    public void deletePelanggan(UUID id) {
        PelangganServis pelanggan = getPelangganById(id);
        pelangganServisRepository.delete(pelanggan);
    }

    // --- READ (ALL WITH PAGINATION & SEARCH) ---
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PelangganServis> getAllPelanggan(String search, int page, int size) {
        
        // Atur parameter pagination dan urutkan dari data terbaru
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        // Jika ada kata kunci pencarian, gunakan custom query. Jika tidak, tampilkan semua.
        if (search != null && !search.trim().isEmpty()) {
            return pelangganServisRepository.searchByNamaOrTelepon(search, pageable);
        }
        
        return pelangganServisRepository.findAll(pageable);
    }
}