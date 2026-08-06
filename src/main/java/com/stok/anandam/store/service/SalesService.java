package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Sales;
import com.stok.anandam.store.core.postgres.repository.SalesRepository;
import com.stok.anandam.store.dto.ProfitabilityResponse;
import com.stok.anandam.store.dto.ProfitabilityRowResponse;
import com.stok.anandam.store.dto.SalesSummaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SalesService {

        @Autowired
        private SalesRepository salesRepository;

        public SalesSummaryResponse<Sales> getSales(
                        int page, int size, String sortBy, String dir,
                        String startDateStr, String endDateStr, List<String> empCodes, 
                        List<String> categories, String search, String searchColumn) {
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
                Page<Sales> pageResult = salesRepository.findByFilters(start, end, empCodes, categories, search, searchColumn, pageable);

                // 4. Ambil Total Sum
                BigDecimal totalSum = salesRepository.sumGrandTotalByFilters(start, end, empCodes, categories, search, searchColumn);
                Long totalQty = salesRepository.sumQtyByFilters(start, end, empCodes, categories, search, searchColumn);
                BigDecimal totalHpp = salesRepository.sumTotalHppByFilters(start, end, empCodes, categories, search, searchColumn);
                BigDecimal totalLaba = salesRepository.sumLabaKotorByFilters(start, end, empCodes, categories, search, searchColumn);

                // 5. Return Response
                return SalesSummaryResponse.<Sales>builder()
                                .content(pageResult.getContent())
                                .totalGrandSum(totalSum)
                                .totalQty(totalQty != null ? BigDecimal.valueOf(totalQty) : BigDecimal.ZERO)
                                .totalHpp(totalHpp != null ? totalHpp : BigDecimal.ZERO)
                                .totalLabaKotor(totalLaba != null ? totalLaba : BigDecimal.ZERO)
                                .marginPct(marginPct(totalSum, totalLaba))
                                .totalPages(pageResult.getTotalPages())
                                .totalElements(pageResult.getTotalElements())
                                .build();
        }

        public java.util.List<Sales> getAllSalesForExport(String startDateStr, String endDateStr, List<String> empCodes,
                        List<String> categories, String search, String searchColumn) {
                LocalDate start = (startDateStr != null && !startDateStr.isBlank())
                                ? LocalDate.parse(startDateStr)
                                : LocalDate.of(2000, 1, 1);

                LocalDate end = (endDateStr != null && !endDateStr.isBlank())
                                ? LocalDate.parse(endDateStr)
                                : LocalDate.now();

                return salesRepository.findAllByFilters(start, end, empCodes, categories, search, searchColumn);
        }

        public java.util.List<com.stok.anandam.store.dto.EmployeeOption> getEmployeeCodes() {
                return salesRepository.findDistinctEmpCodeAndName().stream()
                                .map(obj -> com.stok.anandam.store.dto.EmployeeOption.builder()
                                                .empCode((String) obj[0])
                                                .empName((String) obj[1])
                                                .build())
                                .collect(java.util.stream.Collectors.toList());
        }

        public java.util.List<String> getDeptCodes() {
                return salesRepository.findDistinctDepCodes();
        }

        /**
         * Laporan Profitabilitas Penjualan per barang.
         * Source: tabel sales (Postgres) yang sudah diisi kolom hpp_satuan/total_hpp/laba_kotor
         * oleh service migrasi. Retur (RJ) sudah diperhitungkan di level baris.
         */
        public ProfitabilityResponse<ProfitabilityRowResponse> getProfitability(
                        String startDateStr, String endDateStr, List<String> empCodes,
                        List<String> categories, String search, String searchColumn) {

                LocalDate start = (startDateStr != null && !startDateStr.isBlank())
                                ? LocalDate.parse(startDateStr)
                                : LocalDate.of(2000, 1, 1);

                LocalDate end = (endDateStr != null && !endDateStr.isBlank())
                                ? LocalDate.parse(endDateStr)
                                : LocalDate.now();

                List<Object[]> rows = salesRepository.findProfitabilityByFilters(start, end, empCodes, categories, search, searchColumn);

                List<ProfitabilityRowResponse> content = rows.stream().map(row -> {
                        BigDecimal omset = num((Object) row[5]);
                        BigDecimal totalHpp = num((Object) row[6]);
                        BigDecimal labaKotor = num((Object) row[7]);

                        return ProfitabilityRowResponse.builder()
                                        .itemCode((String) row[0])
                                        .itemName((String) row[1])
                                        .depCode((String) row[2])
                                        .depName((String) row[3])
                                        .qty(num((Object) row[4]))
                                        .omset(omset)
                                        .totalHpp(totalHpp)
                                        .labaKotor(labaKotor)
                                        .marginPct(marginPct(omset, labaKotor))
                                        .build();
                }).collect(Collectors.toList());

                // Total footer dihitung langsung dari baris hasil (tidak lewat query agregat
                // tambahan, karena proyeksi Object[] agregat tanpa GROUP BY di Hibernate
                // bisa dikembalikan sebagai array dengan panjang tidak terduga).
                BigDecimal totalQty = BigDecimal.ZERO;
                BigDecimal totalOmset = BigDecimal.ZERO;
                BigDecimal totalHpp = BigDecimal.ZERO;
                BigDecimal totalLaba = BigDecimal.ZERO;
                for (ProfitabilityRowResponse r : content) {
                        totalQty = totalQty.add(val(r.getQty()));
                        totalOmset = totalOmset.add(val(r.getOmset()));
                        totalHpp = totalHpp.add(val(r.getTotalHpp()));
                        totalLaba = totalLaba.add(val(r.getLabaKotor()));
                }

                return ProfitabilityResponse.<ProfitabilityRowResponse>builder()
                                .content(content)
                                .totalQty(totalQty)
                                .totalOmset(totalOmset)
                                .totalHpp(totalHpp)
                                .totalLabaKotor(totalLaba)
                                .marginPct(marginPct(totalOmset, totalLaba))
                                .build();
        }

        /** Konversi nilai SUM (Long/BigDecimal/null) menjadi BigDecimal aman. */
        private BigDecimal num(Object o) {
                if (o == null)
                        return BigDecimal.ZERO;
                if (o instanceof BigDecimal bd)
                        return bd;
                if (o instanceof Number n)
                        return BigDecimal.valueOf(n.doubleValue());
                return BigDecimal.ZERO;
        }

        /** BigDecimal non-null helper untuk penjumlahan. */
        private BigDecimal val(BigDecimal v) {
                return v == null ? BigDecimal.ZERO : v;
        }

        /** margin % = labaKotor / omset * 100 (2 desimal, hindari pembagian nol). */
        private BigDecimal marginPct(BigDecimal omset, BigDecimal laba) {
                if (omset == null || omset.compareTo(BigDecimal.ZERO) == 0)
                        return BigDecimal.ZERO;
                return laba.multiply(BigDecimal.valueOf(100))
                                .divide(omset, 2, RoundingMode.HALF_UP);
        }
}