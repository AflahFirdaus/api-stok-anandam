package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.Stock;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private BigDecimal totalSalesToday;
    private BigDecimal totalPurchasesToday;
    private long totalVisitsToday;
    private long totalLowStockItems; // <--- Pastikan pakai 'long'
    private List<Stock> lowStockPreview;
    private long totalTkdnItems;
}