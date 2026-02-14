package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SalesSummaryResponse<T> {
    private BigDecimal totalGrandSum; // Total Omset Penjualan
    private List<T> content;          // List Data Penjualan
    private int totalPages;
    private long totalElements;
}