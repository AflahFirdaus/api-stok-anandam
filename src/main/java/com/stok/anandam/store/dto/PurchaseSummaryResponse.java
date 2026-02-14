package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PurchaseSummaryResponse<T> {
    private BigDecimal totalGrandSum; // Total belanja di periode ini
    private List<T> content;          // List datanya
    private int totalPages;           // Info paging tambahan
    private long totalElements;
}