package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ringkasan omzet per marketing untuk satu periode terpilih.
 * Berisi list baris per marketing + total (footer) dari kumpulan baris tsb.
 */
@Data
@Builder
public class MarketingSalesSummaryResponse {

    private String period;              // DAY | WEEK | MONTH | YEAR
    private LocalDateRange range;       // Rentang tanggal yang dipakai untuk filter
    private List<MarketingSalesRow> content;

    // Total footer (dihitung dari content)
    private BigDecimal totalQty;
    private BigDecimal totalOmset;
    private BigDecimal totalHpp;
    private BigDecimal totalLabaKotor;
    private BigDecimal marginPct;

    @Data
    @Builder
    public static class LocalDateRange {
        private java.time.LocalDate start;
        private java.time.LocalDate end;
    }
}
