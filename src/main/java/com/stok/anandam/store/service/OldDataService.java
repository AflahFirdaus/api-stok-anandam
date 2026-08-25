package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.OldItemSerialNumber;
import com.stok.anandam.store.core.postgres.model.OldPurchase;
import com.stok.anandam.store.core.postgres.model.OldSales;
import com.stok.anandam.store.core.postgres.repository.OldItemSnRepository;
import com.stok.anandam.store.core.postgres.repository.OldPurchaseRepository;
import com.stok.anandam.store.core.postgres.repository.OldSalesRepository;
import com.stok.anandam.store.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

        // ==================== LAPORAN OMSET PER MARKETING (OLD SALES) ====================

        public MarketingSalesSummaryResponse getMarketingSalesSummary(
                        String period, LocalDate date, LocalDate startDate, LocalDate endDate, List<String> empCodes) {
                PeriodRange range = resolveRange(period, date, startDate, endDate);
                List<String> filter = normalizeEmpCodes(empCodes);

                List<MarketingSalesRow> content = oldSalesRepository
                                .sumMarketingSalesByFilters(range.start, range.end, filter).stream()
                                .map(row -> {
                                        BigDecimal omset = num(row[3]);
                                        BigDecimal hpp = num(row[4]);
                                        BigDecimal laba = num(row[5]);
                                        return MarketingSalesRow.builder()
                                                        .empCode(((Object) row[0]) == null ? null : (String) row[0])
                                                        .empName(((Object) row[1]) == null ? null : (String) row[1])
                                                        .qty(num(row[2]))
                                                        .omset(omset)
                                                        .totalHpp(hpp)
                                                        .labaKotor(laba)
                                                        .marginPct(marginPct(omset, laba))
                                                        .build();
                                })
                                .collect(Collectors.toList());

                BigDecimal totalOmset = BigDecimal.ZERO;
                BigDecimal totalHpp = BigDecimal.ZERO;
                BigDecimal totalLaba = BigDecimal.ZERO;
                BigDecimal totalQty = BigDecimal.ZERO;
                for (MarketingSalesRow r : content) {
                        totalOmset = totalOmset.add(val(r.getOmset()));
                        totalHpp = totalHpp.add(val(r.getTotalHpp()));
                        totalLaba = totalLaba.add(val(r.getLabaKotor()));
                        totalQty = totalQty.add(val(r.getQty()));
                }

                return MarketingSalesSummaryResponse.builder()
                                .period(period.toUpperCase())
                                .range(MarketingSalesSummaryResponse.LocalDateRange.builder()
                                                .start(range.start).end(range.end).build())
                                .content(content)
                                .totalQty(totalQty)
                                .totalOmset(totalOmset)
                                .totalHpp(totalHpp)
                                .totalLabaKotor(totalLaba)
                                .marginPct(marginPct(totalOmset, totalLaba))
                                .build();
        }

        public MarketingNotaDetailResponse getMarketingSalesNotaDetail(
                        String period, LocalDate date, LocalDate startDate, LocalDate endDate, String empCode) {
                PeriodRange range = resolveRange(period, date, startDate, endDate);
                List<String> filter = normalizeEmpCodes(empCode == null ? null : Collections.singletonList(empCode));

                List<MarketingNotaRow> content = oldSalesRepository
                                .findNotaByFilters(range.start, range.end, filter).stream()
                                .map(row -> {
                                        BigDecimal omset = num(row[5]);
                                        BigDecimal hpp = num(row[6]);
                                        BigDecimal laba = num(row[7]);
                                        return MarketingNotaRow.builder()
                                                        .docNo((String) row[0])
                                                        .docDate(toLocalDate(row[1]))
                                                        .code((String) row[2])
                                                        .parName((String) row[3])
                                                        .qty(num(row[4]))
                                                        .omset(omset)
                                                        .totalHpp(hpp)
                                                        .labaKotor(laba)
                                                        .marginPct(marginPct(omset, laba))
                                                        .empCode((String) row[8])
                                                        .empName((String) row[9])
                                                        .build();
                                })
                                .collect(Collectors.toList());

                BigDecimal totalOmset = BigDecimal.ZERO;
                BigDecimal totalHpp = BigDecimal.ZERO;
                BigDecimal totalLaba = BigDecimal.ZERO;
                BigDecimal totalQty = BigDecimal.ZERO;
                for (MarketingNotaRow r : content) {
                        totalOmset = totalOmset.add(val(r.getOmset()));
                        totalHpp = totalHpp.add(val(r.getTotalHpp()));
                        totalLaba = totalLaba.add(val(r.getLabaKotor()));
                        totalQty = totalQty.add(val(r.getQty()));
                }

                String resolvedName = (empCode != null && !empCode.isBlank() && !content.isEmpty())
                                ? content.get(0).getEmpName() : null;

                return MarketingNotaDetailResponse.builder()
                                .empCode(empCode)
                                .empName(resolvedName)
                                .period(period.toUpperCase())
                                .start(range.start)
                                .end(range.end)
                                .content(content)
                                .totalQty(totalQty)
                                .totalOmset(totalOmset)
                                .totalHpp(totalHpp)
                                .totalLabaKotor(totalLaba)
                                .marginPct(marginPct(totalOmset, totalLaba))
                                .build();
        }

        public MarketingItemDetailResponse getMarketingSalesItemDetail(
                        String period, LocalDate date, LocalDate startDate, LocalDate endDate, String empCode) {
                PeriodRange range = resolveRange(period, date, startDate, endDate);
                List<String> filter = normalizeEmpCodes(empCode == null ? null : Collections.singletonList(empCode));

                List<MarketingItemRow> content = oldSalesRepository
                                .findItemByFilters(range.start, range.end, filter).stream()
                                .map(row -> {
                                        BigDecimal omset = num(row[2]);
                                        BigDecimal hpp = num(row[3]);
                                        BigDecimal laba = num(row[4]);
                                        return MarketingItemRow.builder()
                                                        .itemName((String) row[0])
                                                        .qty(num(row[1]))
                                                        .omset(omset)
                                                        .totalHpp(hpp)
                                                        .labaKotor(laba)
                                                        .marginPct(marginPct(omset, laba))
                                                        .empCode((String) row[5])
                                                        .empName((String) row[6])
                                                        .build();
                                })
                                .collect(Collectors.toList());

                BigDecimal totalOmset = BigDecimal.ZERO;
                BigDecimal totalHpp = BigDecimal.ZERO;
                BigDecimal totalLaba = BigDecimal.ZERO;
                BigDecimal totalQty = BigDecimal.ZERO;
                for (MarketingItemRow r : content) {
                        totalOmset = totalOmset.add(val(r.getOmset()));
                        totalHpp = totalHpp.add(val(r.getTotalHpp()));
                        totalLaba = totalLaba.add(val(r.getLabaKotor()));
                        totalQty = totalQty.add(val(r.getQty()));
                }

                String resolvedName = (empCode != null && !empCode.isBlank() && !content.isEmpty())
                                ? content.get(0).getEmpName() : null;

                return MarketingItemDetailResponse.builder()
                                .empCode(empCode)
                                .empName(resolvedName)
                                .period(period.toUpperCase())
                                .start(range.start)
                                .end(range.end)
                                .content(content)
                                .totalQty(totalQty)
                                .totalOmset(totalOmset)
                                .totalHpp(totalHpp)
                                .totalLabaKotor(totalLaba)
                                .marginPct(marginPct(totalOmset, totalLaba))
                                .build();
        }

        private BigDecimal marginPct(BigDecimal omset, BigDecimal laba) {
                if (omset == null || omset.compareTo(BigDecimal.ZERO) == 0)
                        return BigDecimal.ZERO;
                return laba.multiply(BigDecimal.valueOf(100))
                                .divide(omset, 2, java.math.RoundingMode.HALF_UP);
        }

        // ==================== HELPER METHODS ====================

        private BigDecimal num(Object o) {
                if (o == null) return BigDecimal.ZERO;
                if (o instanceof BigDecimal bd) return bd;
                if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
                return BigDecimal.ZERO;
        }

        private BigDecimal val(BigDecimal v) {
                return v == null ? BigDecimal.ZERO : v;
        }

        private LocalDate toLocalDate(Object o) {
                if (o == null) return null;
                if (o instanceof LocalDate ld) return ld;
                if (o instanceof java.sql.Date sd) return sd.toLocalDate();
                if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
                if (o instanceof java.util.Date ud) return new java.sql.Date(ud.getTime()).toLocalDate();
                if (o instanceof String s && !s.isBlank()) {
                        try {
                                return LocalDate.parse(s);
                        } catch (Exception ignored) {}
                }
                return null;
        }

        private List<String> normalizeEmpCodes(List<String> empCodes) {
                if (empCodes == null || empCodes.isEmpty()) return null;
                List<String> cleaned = new java.util.ArrayList<>();
                for (String e : empCodes) {
                        if (e != null && !e.trim().isEmpty()) {
                                String upper = e.trim().toUpperCase();
                                String underscored = upper.replace(" ", "_");
                                if (!cleaned.contains(underscored)) cleaned.add(underscored);
                                if (!cleaned.contains(upper)) cleaned.add(upper);
                        }
                }
                return cleaned.isEmpty() ? null : cleaned;
        }

        private PeriodRange resolveRange(String period, LocalDate date, LocalDate startDate, LocalDate endDate) {
                if (startDate != null && endDate != null) {
                        return new PeriodRange(startDate, endDate);
                }
                LocalDate ref = date != null ? date : LocalDate.now();
                String p = period == null ? "" : period.toUpperCase();

                if ("WEEK".equals(p)) {
                        LocalDate start = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                        return new PeriodRange(start, start.plusDays(6));
                } else if ("MONTH".equals(p)) {
                        LocalDate start = ref.withDayOfMonth(1);
                        return new PeriodRange(start, ref.with(TemporalAdjusters.lastDayOfMonth()));
                } else if ("YEAR".equals(p)) {
                        LocalDate start = ref.withDayOfYear(1);
                        return new PeriodRange(start, ref.with(TemporalAdjusters.lastDayOfYear()));
                } else {
                        return new PeriodRange(ref, ref);
                }
        }

        private static class PeriodRange {
                final LocalDate start;
                final LocalDate end;
                PeriodRange(LocalDate start, LocalDate end) {
                        this.start = start;
                        this.end = end;
                }
        }
}
