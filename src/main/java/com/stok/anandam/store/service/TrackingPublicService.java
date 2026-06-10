package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.RiwayatTracking;
import com.stok.anandam.store.core.postgres.model.TransaksiServis;
import com.stok.anandam.store.core.postgres.repository.RiwayatTrackingRepository;
import com.stok.anandam.store.core.postgres.repository.TransaksiServisRepository;
import com.stok.anandam.store.dto.UserTrackingResponse;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackingPublicService {

    private final TransaksiServisRepository transaksiRepository;
    private final RiwayatTrackingRepository riwayatRepository;

    // --- 1. Cari via Token URL (QR Code) ---
    @Transactional(readOnly = true)
    public UserTrackingResponse getTrackingDataByToken(UUID trackingToken) {
        TransaksiServis transaksi = transaksiRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new ResourceNotFoundException("Data tracking tidak ditemukan atau token tidak valid."));
        
        return buildResponseDto(transaksi);
    }

    // --- 2. Cari via Nomor Telepon (Form Website) ---
    @Transactional(readOnly = true)
    public List<UserTrackingResponse> getTrackingDataByPhone(String noTelepon) {
        List<TransaksiServis> listTransaksi = transaksiRepository.findByPelangganNoTelepon(noTelepon);
        
        if (listTransaksi.isEmpty()) {
            throw new ResourceNotFoundException("Tidak ada riwayat servis untuk nomor telepon: " + noTelepon);
        }

        // Ubah semua transaksi yang ditemukan menjadi bentuk DTO yang aman untuk publik
        return listTransaksi.stream()
                .map(this::buildResponseDto)
                .collect(Collectors.toList());
    }

    // --- HELPER: Data Masking ---
    private String maskNamaPelanggan(String namaAsli) {
        if (namaAsli == null || namaAsli.trim().isEmpty()) return "NN";
        
        String[] parts = namaAsli.trim().split(" ");
        if (parts.length == 1) {
            String word = parts[0];
            return word.length() <= 3 ? word.substring(0, 1) + "***" : word.substring(0, 3) + "***";
        }

        StringBuilder masked = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            masked.append(" ").append(parts[i].charAt(0)).append("***");
        }
        return masked.toString();
    }

    // --- HELPER: Masking Status Internal ke Publik ---
    private String maskStatusUntukPublik(com.stok.anandam.store.core.postgres.model.StatusServis statusAsli) {
        if (statusAsli == null) return "-";
        
        if (statusAsli.name().startsWith("KLAIM_")) {
            return "DITANGANI DISTRIBUTOR/KLAIM GARANSI";
        }
        return statusAsli.getValue(); 
    }

    // --- HELPER: Mengubah Entitas ke DTO Publik ---
    private UserTrackingResponse buildResponseDto(TransaksiServis transaksi) {
        List<RiwayatTracking> riwayatList = riwayatRepository.findByTransaksiIdOrderByWaktuUpdateDesc(transaksi.getId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        
        List<UserTrackingResponse.TimelineLog> timelineLogs = riwayatList.stream()
                .map(log -> UserTrackingResponse.TimelineLog.builder()
                        .status(maskStatusUntukPublik(log.getStatusLog())) // <-- PANGGIL DI SINI
                        .catatan(log.getCatatanPublik())
                        .waktuUpdate(log.getWaktuUpdate().format(formatter))
                        .build())
                .collect(Collectors.toList());

        return UserTrackingResponse.builder()
                .noServis(transaksi.getNoServis())
                .namaPelangganMasked(maskNamaPelanggan(transaksi.getPelanggan().getNamaPelanggan()))
                .jenisBarang(transaksi.getJenisBarang())
                .merek(transaksi.getMerek())
                .modelSeri(transaksi.getModelSeri())
                .kerusakan(transaksi.getKerusakan())
                .statusTerkini(maskStatusUntukPublik(transaksi.getStatusTerkini())) // <-- PANGGIL DI SINI
                .garansi(transaksi.getDurasiGaransi() != null ? transaksi.getDurasiGaransi() : "-")
                .timeline(timelineLogs)
                .build();
    }
}