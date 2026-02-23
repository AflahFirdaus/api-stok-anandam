package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Satu baris ringkasan (parent atau child): NAMA, STOK, PRESENTASE.
 * Jika parent punya sub kategori, children diisi; jika tidak, children kosong.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockSummaryRowResponse {
    private String nama;
    private BigDecimal stok;
    private BigDecimal presentase;
    /** Sub kategori (untuk parent); kosong untuk child atau kategori standalone. */
    @Builder.Default
    private List<StockSummaryRowResponse> children = List.of();
}
