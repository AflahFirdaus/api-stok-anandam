package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.KlaimDistributor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlaimDistributorResponse {
    private UUID id;
    private UUID transaksiId;
    private String namaDistributor;
    private String alamatDistributor;
    private LocalDateTime tanggalKirim;
    private LocalDateTime tanggalKembali;
    private String resiPengiriman;
    private BigDecimal biayaKlaim;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KlaimDistributorResponse fromEntity(KlaimDistributor klaim) {
        if (klaim == null) return null;

        return KlaimDistributorResponse.builder()
                .id(klaim.getId())
                .transaksiId(klaim.getTransaksi() != null ? klaim.getTransaksi().getId() : null)
                .namaDistributor(klaim.getNamaDistributor())
                .alamatDistributor(klaim.getAlamatDistributor())
                .tanggalKirim(klaim.getTanggalKirim())
                .tanggalKembali(klaim.getTanggalKembali())
                .resiPengiriman(klaim.getResiPengiriman())
                .biayaKlaim(klaim.getBiayaKlaim())
                .createdBy(klaim.getCreatedBy() != null ? klaim.getCreatedBy().getNama() : null)
                .createdAt(klaim.getCreatedAt())
                .updatedAt(klaim.getUpdatedAt())
                .build();
    }
}