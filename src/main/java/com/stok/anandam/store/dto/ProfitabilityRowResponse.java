package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProfitabilityRowResponse {

    private String itemCode;   // Kode barang (ite_code)
    private String itemName;   // Nama barang
    private String depCode;    // Kode kategori
    private String depName;    // Nama kategori

    private BigDecimal qty;        // Total quantity terjual
    private BigDecimal omset;      // Total penjualan (grand_total)
    private BigDecimal totalHpp;   // Total Harga Pokok Penjualan
    private BigDecimal labaKotor;  // Laba kotor = omset - total HPP
    private BigDecimal marginPct;  // Persentase margin
}
