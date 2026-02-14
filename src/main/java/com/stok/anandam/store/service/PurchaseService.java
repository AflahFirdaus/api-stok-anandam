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

@Service
public class PurchaseService {

    @Autowired
    private PurchaseRepository purchaseRepository;

    public PurchaseSummaryResponse<Purchase> getPurchases(
            int page, int size, String sortBy, String dir, 
            String startDateStr, String endDateStr, String search
    ) {
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
        Page<Purchase> pageResult = purchaseRepository.findByDateRangeAndSearch(start, end, search, pageable);

        // 4. Ambil Total Sum (Uang)
        BigDecimal totalSum = purchaseRepository.sumGrandTotalByDateRange(start, end, search);

        // 5. Return DTO Summary
        return PurchaseSummaryResponse.<Purchase>builder()
                .content(pageResult.getContent())
                .totalGrandSum(totalSum)
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .build();
    }
}