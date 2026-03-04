package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.OldItemSerialNumber;
import com.stok.anandam.store.core.postgres.model.OldPurchase;
import com.stok.anandam.store.core.postgres.model.OldSales;
import com.stok.anandam.store.core.postgres.repository.OldItemSnRepository;
import com.stok.anandam.store.core.postgres.repository.OldPurchaseRepository;
import com.stok.anandam.store.core.postgres.repository.OldSalesRepository;
import com.stok.anandam.store.dto.SalesSummaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class OldDataService {

        @Autowired
        private OldSalesRepository oldSalesRepository;

        @Autowired
        private OldPurchaseRepository oldPurchaseRepository;

        @Autowired
        private OldItemSnRepository oldItemSnRepository;

        public SalesSummaryResponse<OldSales> getSales(
                        int page, int size, String sortBy, String dir,
                        String startDateStr, String endDateStr, String empCode, String search) {
                LocalDate start = (startDateStr != null && !startDateStr.isBlank())
                                ? LocalDate.parse(startDateStr)
                                : LocalDate.of(2000, 1, 1);

                LocalDate end = (endDateStr != null && !endDateStr.isBlank())
                                ? LocalDate.parse(endDateStr)
                                : LocalDate.now();

                Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

                Page<OldSales> pageResult = oldSalesRepository.findByFilters(start, end, empCode, search, pageable);
                BigDecimal totalSum = oldSalesRepository.sumGrandTotalByFilters(start, end, empCode, search);
                BigDecimal totalQty = oldSalesRepository.sumQtyByFilters(start, end, empCode, search);

                return SalesSummaryResponse.<OldSales>builder()
                                .content(pageResult.getContent())
                                .totalGrandSum(totalSum)
                                .totalQty(totalQty)
                                .totalPages(pageResult.getTotalPages())
                                .totalElements(pageResult.getTotalElements())
                                .build();
        }

        public SalesSummaryResponse<OldPurchase> getPurchase(
                        int page, int size, String sortBy, String dir,
                        String startDateStr, String endDateStr, String search) {
                LocalDate start = (startDateStr != null && !startDateStr.isBlank())
                                ? LocalDate.parse(startDateStr)
                                : LocalDate.of(2000, 1, 1);

                LocalDate end = (endDateStr != null && !endDateStr.isBlank())
                                ? LocalDate.parse(endDateStr)
                                : LocalDate.now();

                Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

                Page<OldPurchase> pageResult = oldPurchaseRepository.findByFilters(start, end, search, pageable);
                BigDecimal totalSum = oldPurchaseRepository.sumGrandTotalByFilters(start, end, search);
                BigDecimal totalQty = oldPurchaseRepository.sumQtyByFilters(start, end, search);

                return SalesSummaryResponse.<OldPurchase>builder()
                                .content(pageResult.getContent())
                                .totalGrandSum(totalSum)
                                .totalQty(totalQty)
                                .totalPages(pageResult.getTotalPages())
                                .totalElements(pageResult.getTotalElements())
                                .build();
        }

        public Page<OldItemSerialNumber> getItemSn(
                        int page, int size, String sortBy, String dir,
                        String type, String search, String startDateStr, String endDateStr) {
                LocalDateTime start = (startDateStr != null && !startDateStr.isBlank())
                                ? LocalDate.parse(startDateStr).atStartOfDay()
                                : LocalDateTime.of(2000, 1, 1, 0, 0);

                LocalDateTime end = (endDateStr != null && !endDateStr.isBlank())
                                ? LocalDate.parse(endDateStr).atTime(LocalTime.MAX)
                                : LocalDateTime.now();

                Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

                return oldItemSnRepository.findByFilters(start, end, type, search, pageable);
        }

        public java.util.List<String> getEmployeeCodes() {
                return oldSalesRepository.findDistinctEmpCodeOrderByEmpCode();
        }

        public java.util.Map<String, Object> getDateRangeMeta() {
                java.util.Map<String, Object> meta = new java.util.HashMap<>();

                int salesMax = oldSalesRepository.findMaxDocDate()
                                .map(d -> d.getYear())
                                .orElse(java.time.LocalDate.now().getYear());

                int purchaseMax = oldPurchaseRepository.findMaxDocDate()
                                .map(d -> d.getYear())
                                .orElse(java.time.LocalDate.now().getYear());

                int itemSnMax = oldItemSnRepository.findMaxTanggal()
                                .map(dt -> dt.getYear())
                                .orElse(java.time.LocalDate.now().getYear());

                meta.put("salesMaxYear", salesMax);
                meta.put("purchaseMaxYear", purchaseMax);
                meta.put("itemSnMaxYear", itemSnMax);
                meta.put("minYear", 2016);
                return meta;
        }
}
