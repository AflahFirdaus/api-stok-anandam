package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Satu baris detail per barang (item) untuk laporan drill-down omset per marketing.
 */
@Data
@Builder
public class MarketingItemRow {
    private String iteCode;      // Kode barang
    private String itemName;     // Nama barang
    private String depCode;      // Kode departemen/kategori
    private String depName;      // Nama departemen/kategori
    private BigDecimal qty;      // Total quantity terjual
    private BigDecimal omset;    // Total penjualan (grand_total)
    private BigDecimal totalHpp; // Total HPP
    private BigDecimal labaKotor;// Laba kotor
    private BigDecimal marginPct;// Margin kotor (%)

    // Info marketing (agar baris item berdiri sendiri)
    private String empCode;
    private String empName;
}
