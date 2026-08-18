package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Satu baris detail per nota (doc) untuk laporan drill-down omset per marketing.
 */
@Data
@Builder
public class MarketingNotaRow {
    private String docNo;        // Nomor nota / dokumen
    private LocalDate docDate;   // Tanggal nota
    private String code;         // Kode partner/pelanggan
    private String parName;      // Nama pelanggan/distributor
    private BigDecimal qty;      // Total quantity pada nota
    private BigDecimal omset;    // Total grand_total pada nota
    private BigDecimal totalHpp; // Total HPP pada nota
    private BigDecimal labaKotor;// Laba kotor pada nota
    private BigDecimal marginPct;// Margin kotor (%)

    // Info marketing (agar baris nota berdiri sendiri)
    private String empCode;
    private String empName;
}
