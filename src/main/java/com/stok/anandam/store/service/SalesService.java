package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Sales;
import com.stok.anandam.store.core.postgres.repository.SalesRepository;
import com.stok.anandam.store.dto.SalesSummaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class SalesService {

    @Autowired
    private SalesRepository salesRepository;

    public SalesSummaryResponse<Sales> getSales(

            int page, int size, String sortBy, String dir,
            String startDateStr, String endDateStr, String empCode, String search
    ) {
        // 1. Parsing Tanggal
        LocalDate start = (startDateStr != null && !startDateStr.isBlank()) 
                ? LocalDate.parse(startDateStr) 
                : LocalDate.of(2000, 1, 1);

        LocalDate end = (endDateStr != null && !endDateStr.isBlank()) 
                ? LocalDate.parse(endDateStr) 
                : LocalDate.now();

        // 2. Setup Paging
        Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // 3. Ambil Data
        Page<Sales> pageResult = salesRepository.findByFilters(start, end, empCode, search, pageable);

        // 4. Ambil Total Sum
        BigDecimal totalSum = salesRepository.sumGrandTotalByFilters(start, end, empCode, search);

        // 5. Return Response
        return SalesSummaryResponse.<Sales>builder()
                .content(pageResult.getContent())
                .totalGrandSum(totalSum)
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .build();
    }

    public java.util.List<String> getEmployeeCodes() {
        return salesRepository.findDistinctEmpCodeOrderByEmpCode();
    }
}