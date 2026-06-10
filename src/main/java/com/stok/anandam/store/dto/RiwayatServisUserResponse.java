package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.PelangganServis;
import com.stok.anandam.store.core.postgres.model.TransaksiServis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiwayatServisUserResponse {

    // Informasi Pelanggan
    private UUID pelangganId;
    private String namaPelanggan;
    private String noTelepon;
    private String noWhatsapp;
    private String alamat;

    // Rekapan
    private int totalServis;
    private BigDecimal totalBiayaFinal;
    private BigDecimal totalPendapatanBersih; // biayaFinal - modalSparepart

    // Detail transaksi
    private List<TransaksiServisRingkas> daftarServis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransaksiServisRingkas {
        private UUID id;
        private String noServis;
        private String jenisBarang;
        private String merek;
        private String modelSeri;
        private String kerusakan;
        private String kondisiServis;
        private String ketTindakan;
        private BigDecimal estimasiBiaya;
        private BigDecimal biayaFinal;
        private BigDecimal modalSparepart;
        private String statusTerkini;
        private String statusBayar;
        private LocalDateTime tglTerima;
        private LocalDateTime tglAmbil;
        private LocalDateTime createdAt;

        public static TransaksiServisRingkas fromEntity(TransaksiServis t) {
            return TransaksiServisRingkas.builder()
                    .id(t.getId())
                    .noServis(t.getNoServis())
                    .jenisBarang(t.getJenisBarang())
                    .merek(t.getMerek())
                    .modelSeri(t.getModelSeri())
                    .kerusakan(t.getKerusakan())
                    .kondisiServis(t.getKondisiServis())
                    .ketTindakan(t.getKetTindakan())
                    .estimasiBiaya(t.getEstimasiBiaya())
                    .biayaFinal(t.getBiayaFinal())
                    .modalSparepart(t.getModalSparepart())
                    .statusTerkini(t.getStatusTerkini() != null ? t.getStatusTerkini().getValue() : null)
                    .statusBayar(t.getStatusBayar() != null ? t.getStatusBayar().name() : null)
                    .tglTerima(t.getTglTerima())
                    .tglAmbil(t.getTglAmbil())
                    .createdAt(t.getCreatedAt())
                    .build();
        }
    }

    public static RiwayatServisUserResponse fromEntity(PelangganServis pelanggan, List<TransaksiServis> transaksiList) {
        if (pelanggan == null) return null;

        BigDecimal totalBiayaFinal = transaksiList.stream()
                .filter(t -> t.getBiayaFinal() != null)
                .map(TransaksiServis::getBiayaFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalModalSparepart = transaksiList.stream()
                .filter(t -> t.getModalSparepart() != null)
                .map(TransaksiServis::getModalSparepart)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransaksiServisRingkas> daftar = transaksiList.stream()
                .map(TransaksiServisRingkas::fromEntity)
                .collect(Collectors.toList());

        return RiwayatServisUserResponse.builder()
                .pelangganId(pelanggan.getId())
                .namaPelanggan(pelanggan.getNamaPelanggan())
                .noTelepon(pelanggan.getNoTelepon())
                .noWhatsapp(pelanggan.getNoWhatsapp())
                .alamat(pelanggan.getAlamat())
                .totalServis(transaksiList.size())
                .totalBiayaFinal(totalBiayaFinal)
                .totalPendapatanBersih(totalBiayaFinal.subtract(totalModalSparepart))
                .daftarServis(daftar)
                .build();
    }
}