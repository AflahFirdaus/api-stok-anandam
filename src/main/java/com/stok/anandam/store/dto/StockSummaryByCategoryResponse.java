package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Satu baris ringkasan stok per kategori: NAMA, STOK (total grand_total), PROSENTASE (%).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockSummaryByCategoryResponse {
    /** Nama kategori (kategori_itemcode atau kategori_nama). */
    private String nama;
    /** Jumlah total grand_total untuk kategori ini. */
    private BigDecimal stok;
    /** Persentase terhadap total semua stok (0–100). */
    private BigDecimal presentase;
}
