package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Purchase;
import com.stok.anandam.store.core.postgres.repository.PurchaseRepository;
import com.stok.anandam.store.dto.PurchaseSummaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PurchaseService {

        @Autowired
        private PurchaseRepository purchaseRepository;

        public PurchaseSummaryResponse<Purchase> getPurchases(
                        int page, int size, String sortBy, String dir,
                        String startDateStr, String endDateStr, List<String> categories, String search, String searchColumn) {
                // 1. Parsing Tanggal (Default: Tahun 2000 s/d Hari Ini jika kosong)
                LocalDate start = (startDateStr != null && !startDateStr.isBlank())
                                ? LocalDate.parse(startDateStr)
                                : LocalDate.of(2016, 1, 1);

                LocalDate end = (endDateStr != null && !endDateStr.isBlank())
                                ? LocalDate.parse(endDateStr)
                                : LocalDate.now();

                // 2. Setup Paging & Sorting
                Sort.Direction direction = dir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

                // 3. Ambil List Data
                Page<Purchase> pageResult = purchaseRepository.findByDateRangeAndSearch(start, end, categories, search, searchColumn, pageable);

                // 4. Ambil Total Sum (Uang)
                BigDecimal totalSum = purchaseRepository.sumGrandTotalByDateRange(start, end, categories, search, searchColumn);
                Long totalQty = purchaseRepository.sumQtyByDateRange(start, end, categories, search, searchColumn);

                // 5. Return DTO Summary
                return PurchaseSummaryResponse.<Purchase>builder()
                                .content(pageResult.getContent())
                                .totalGrandSum(totalSum)
                                .totalQty(totalQty != null ? BigDecimal.valueOf(totalQty) : BigDecimal.ZERO)
                                .totalPages(pageResult.getTotalPages())
                                .totalElements(pageResult.getTotalElements())
                                .build();
        }

        public java.util.List<Purchase> getAllPurchasesForExport(String startDateStr, String endDateStr,
                        List<String> categories, String search, String searchColumn) {
                LocalDate start = (startDateStr != null && !startDateStr.isBlank())
                                ? LocalDate.parse(startDateStr)
                                : LocalDate.of(2016, 1, 1);

                LocalDate end = (endDateStr != null && !endDateStr.isBlank())
                                ? LocalDate.parse(endDateStr)
                                : LocalDate.now();

                return purchaseRepository.findAllByDateRangeAndSearch(start, end, categories, search, searchColumn);
        }

        public java.util.List<String> getDeptCodes() {
                return purchaseRepository.findDistinctDepCodes();
        }
}