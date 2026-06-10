package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.StatusPembayaran;
import com.stok.anandam.store.core.postgres.model.StatusServis;
import com.stok.anandam.store.core.postgres.model.TransaksiServis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaksiServisResponse {

    private UUID id;
    private String noServis;
    private UUID trackingToken;
    
    // Pelanggan Info
    private UUID pelangganId;
    private String namaPelanggan;
    private String noTelepon;
    
    // Barang Info
    private String jenisBarang;
    private String merek;
    private String modelSeri;
    private String modelSeriLama;
    private String kelengkapan;
    private String kerusakan;
    
    // Servis Detail
    private BigDecimal dp;
    private BigDecimal estimasiBiaya;
    private StatusServis statusTerkini;
    private String kondisiServis;
    private String ketTindakan;
    
    // Staff Info
    private String namaPenerima;
    private String namaTeknisi;
    private String namaPenyerah;
    
    // Status Waktu
    private LocalDateTime tglTerima;
    private LocalDateTime tglDitangani;
    private LocalDateTime tglAmbil;
    
    // Pengambilan & Finalisasi
    private String pengambilNama;
    private String durasiGaransi;
    private LocalDateTime tglBatasGaransi;
    private Long sisaHariGaransi;
    private BigDecimal biayaFinal;
    private BigDecimal modalSparepart;
    private StatusPembayaran statusBayar;
    private LocalDate tglJatuhTempo;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Menghitung sisa hari garansi secara real-time.
     * Jika tglBatasGaransi null atau sudah lewat, mengembalikan 0.
     */
    private static Long calculateSisaHariGaransi(LocalDateTime tglBatasGaransi) {
        if (tglBatasGaransi == null) return 0L;
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(tglBatasGaransi)) return 0L;
        return ChronoUnit.DAYS.between(now, tglBatasGaransi);
    }

    public static TransaksiServisResponse fromEntity(TransaksiServis transaksi) {
        if (transaksi == null) {
            return null;
        }
        
        return TransaksiServisResponse.builder()
                .id(transaksi.getId())
                .noServis(transaksi.getNoServis())
                .trackingToken(transaksi.getTrackingToken())
                
                .pelangganId(transaksi.getPelanggan() != null ? transaksi.getPelanggan().getId() : null)
                .namaPelanggan(transaksi.getPelanggan() != null ? transaksi.getPelanggan().getNamaPelanggan() : null)
                .noTelepon(transaksi.getPelanggan() != null ? transaksi.getPelanggan().getNoTelepon() : null)
                
                .jenisBarang(transaksi.getJenisBarang())
                .merek(transaksi.getMerek())
                .modelSeri(transaksi.getModelSeri())
                .modelSeriLama(transaksi.getModelSeriLama())
                .kelengkapan(transaksi.getKelengkapan())
                .kerusakan(transaksi.getKerusakan())
                
                .dp(transaksi.getDp())
                .estimasiBiaya(transaksi.getEstimasiBiaya())
                .statusTerkini(transaksi.getStatusTerkini())
                .kondisiServis(transaksi.getKondisiServis())
                .ketTindakan(transaksi.getKetTindakan())
                
                .namaPenerima(transaksi.getPenerima() != null ? transaksi.getPenerima().getNama() : null)
                .namaTeknisi(transaksi.getTeknisi() != null ? transaksi.getTeknisi().getNama() : null)
                .namaPenyerah(transaksi.getPenyerah() != null ? transaksi.getPenyerah().getNama() : null)
                
                .tglTerima(transaksi.getTglTerima())
                .tglDitangani(transaksi.getTglDitangani())
                .tglAmbil(transaksi.getTglAmbil())
                
                .pengambilNama(transaksi.getPengambilNama())
                .durasiGaransi(transaksi.getDurasiGaransi())
                .tglBatasGaransi(transaksi.getTglBatasGaransi())
                .sisaHariGaransi(calculateSisaHariGaransi(transaksi.getTglBatasGaransi()))
                .biayaFinal(transaksi.getBiayaFinal())
                .modalSparepart(transaksi.getModalSparepart())
                .statusBayar(transaksi.getStatusBayar())
                .tglJatuhTempo(transaksi.getTglJatuhTempo())
                
                .createdAt(transaksi.getCreatedAt())
                .updatedAt(transaksi.getUpdatedAt())
                .build();
    }
}
