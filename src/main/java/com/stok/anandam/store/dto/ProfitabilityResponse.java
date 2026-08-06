package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProfitabilityResponse<T> {

    private List<T> content;        // List ringkasan profitabilitas per barang

    private BigDecimal totalQty;        // Total qty laporan
    private BigDecimal totalOmset;      // Total penjualan laporan
    private BigDecimal totalHpp;        // Total HPP laporan
    private BigDecimal totalLabaKotor;  // Total laba kotor laporan
    private BigDecimal marginPct;       // Margin rata-rata laporan
}
