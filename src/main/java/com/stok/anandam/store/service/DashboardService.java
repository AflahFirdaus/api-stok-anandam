package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.repository.*;
import com.stok.anandam.store.dto.DashboardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.stok.anandam.store.dto.EmployeeSalesResponse;

@Service
public class DashboardService {

    @Autowired
    private SalesRepository salesRepo;
    @Autowired
    private PurchaseRepository purchaseRepo;
    @Autowired
    private StockRepository stockRepo;
    @Autowired
    private DataCanvasingRepository canvasRepo;
    @Autowired
    private TkdnRepository tkdnRepo;

    public DashboardResponse getDashboardData() {
        LocalDate today = LocalDate.now();
        int lowStockThreshold = 10;

        return DashboardResponse.builder()
                .totalSalesToday(salesRepo.sumTotalByDate(today))
                .totalPurchasesToday(purchaseRepo.sumTotalByDate(today))
                .totalVisitsToday(canvasRepo.countByTanggal(today))
                .totalLowStockItems(stockRepo.countByFinalStokLessThan(lowStockThreshold))
                .lowStockPreview(stockRepo.findTop5ByLowStock(lowStockThreshold))
                .totalTkdnItems(tkdnRepo.count())
                .totalHpp(stockRepo.sumAllGrandTotal())
                .employeeSalesToday(salesRepo.sumSalesByEmployeeToday(today).stream()
                        .map(obj -> EmployeeSalesResponse.builder()
                                .empCode((String) obj[0])
                                .empName((String) obj[1])
                                .totalSales((BigDecimal) obj[2])
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }
}