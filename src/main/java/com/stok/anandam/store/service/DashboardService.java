package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.repository.*;
import com.stok.anandam.store.dto.DashboardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class DashboardService {

    @Autowired private SalesRepository salesRepo;
    @Autowired private PurchaseRepository purchaseRepo;
    @Autowired private StockRepository stockRepo;
    @Autowired private DataCanvasingRepository canvasRepo;
    @Autowired private TkdnRepository tkdnRepo;

    public DashboardResponse getDashboardData() {
        LocalDate today = LocalDate.now();
        int lowStockThreshold = 10;

        return DashboardResponse.builder()
                .totalSalesToday(salesRepo.sumTotalByDate(today))
                .totalPurchasesToday(purchaseRepo.sumTotalByDate(today))
                .totalVisitsToday(canvasRepo.countByTanggal(today))
                .totalLowStockItems(stockRepo.countByFinalStokLessThan(lowStockThreshold)) // <--- Pakai Stok
                .lowStockPreview(stockRepo.findTop5ByFinalStokLessThanOrderByFinalStokAsc(lowStockThreshold)) // <--- Pakai Stok
                .totalTkdnItems(tkdnRepo.count()) // Total item di katalog TKDN
                .build();
    }
}