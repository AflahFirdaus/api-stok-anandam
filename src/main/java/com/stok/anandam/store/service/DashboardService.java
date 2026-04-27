package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.repository.*;
import com.stok.anandam.store.dto.DashboardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.model.Role;
import com.stok.anandam.store.dto.EmployeeSalesResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private MemoItemRepository memoItemRepository;
    @Autowired
    private PricelistRepository pricelistRepo;

    public DashboardResponse getDashboardData(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        int lowStockThreshold = 10;

        // Fetch active/pending memo items using optimized native query
        com.stok.anandam.store.core.postgres.repository.MemoItemRepository.PendingSummary summary = memoItemRepository.calculatePendingSummaryNative();
        
        long totalPendingStock = (summary != null && summary.getTotalQty() != null) ? summary.getTotalQty() : 0L;
        BigDecimal totalPendingValue = (summary != null && summary.getTotalValue() != null) ? summary.getTotalValue() : BigDecimal.ZERO;

        System.out.println("DEBUG DASHBOARD (Optimized): Pending Stock = " + totalPendingStock + ", Pending Value = " + totalPendingValue);

        DashboardResponse response = DashboardResponse.builder()
                .totalSalesToday(salesRepo.sumTotalByDate(today))
                .totalPurchasesToday(purchaseRepo.sumTotalByDate(today))
                .totalVisitsToday(canvasRepo.countByTanggal(today))
                .totalLowStockItems(stockRepo.countByFinalStokLessThan(lowStockThreshold))
                .lowStockPreview(stockRepo.findTop5ByLowStock(lowStockThreshold))
                .totalTkdnItems(tkdnRepo.count())
                .totalHpp(stockRepo.sumAllGrandTotal())
                .pendingStock(totalPendingStock)
                .pendingValue(totalPendingValue)
                .employeeSalesToday(salesRepo.sumSalesByEmployeeToday(today).stream()
                        .map(obj -> EmployeeSalesResponse.builder()
                                .empCode((String) obj[0])
                                .empName((String) obj[1])
                                .totalSales((BigDecimal) obj[2])
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .employeeSalesMonth(salesRepo.sumSalesByEmployeeMonth(startOfMonth, today).stream()
                        .map(obj -> EmployeeSalesResponse.builder()
                                .empCode((String) obj[0])
                                .empName((String) obj[1])
                                .totalSales((BigDecimal) obj[2])
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .build();

        // FILTER HPP FOR MARKETING
        Role role = user.getRole();
        if (role == Role.MARKETING || 
            role == Role.MARKETING_TOKO || 
            role == Role.MARKETING_PROJECT || 
            role == Role.MARKETING_DISTRIBUSI) {
            response.setTotalHpp(null);
            response.setPendingValue(null);
        }

        return response;
    }
}