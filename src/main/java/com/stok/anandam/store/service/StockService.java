package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Stock;
import com.stok.anandam.store.core.postgres.repository.StockRepository;
import com.stok.anandam.store.dto.StockSummaryByCategoryResponse;
import com.stok.anandam.store.dto.StockSummaryRowResponse;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

    public Page<Stock> getAllStocks(int page, int size, String sortBy, String direction, String search) {
        // 1. Tentukan arah sort (ASC/DESC)
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        
        // 2. Tentukan field untuk sorting (default: itemName)
        Sort sort = Sort.by(sortDirection, sortBy);
        
        // 3. Buat Pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // 4. Cek apakah ada pencarian?
        if (search != null && !search.isEmpty()) {
            // Cari di Item Name ATAU Item Code
            return stockRepository.findByItemNameContainingIgnoreCaseOrItemCodeContainingIgnoreCase(search, search, pageable);
        }

        // 5. Ambil semua data jika tidak ada search
        return stockRepository.findAll(pageable);
    }

    public Stock getStockById(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stok dengan ID " + id + " tidak ditemukan"));
    }

    /**
     * Ringkasan stok per kategori: NAMA, STOK (sum grand_total), PRESENTASE (%). 
     * @param groupBy "kategori_itemcode" (default) atau "kategori_nama"
     * @return Daftar baris + baris terakhir TOTAL dengan presentase 100%
     */
    public List<StockSummaryByCategoryResponse> getSummaryByCategory(String groupBy) {
        List<Object[]> rows;
        if ("kategori_nama".equalsIgnoreCase(groupBy != null ? groupBy : "")) {
            rows = stockRepository.sumGrandTotalByKategoriNama();
        } else {
            rows = stockRepository.sumGrandTotalByKategoriItemcode();
        }

        BigDecimal total = stockRepository.sumAllGrandTotal();
        if (total == null) total = BigDecimal.ZERO;
        if (total.compareTo(BigDecimal.ZERO) == 0) total = BigDecimal.ONE; // hindari bagi nol

        List<StockSummaryByCategoryResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            String nama = row[0] != null ? row[0].toString().trim() : "-";
            BigDecimal stok = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal presentase = stok.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
            result.add(StockSummaryByCategoryResponse.builder()
                    .nama(nama)
                    .stok(stok)
                    .presentase(presentase)
                    .build());
        }
        result.add(StockSummaryByCategoryResponse.builder()
                .nama("TOTAL")
                .stok(total)
                .presentase(new BigDecimal("100.00"))
                .build());
        return result;
    }

    /** Parent → daftar kode sub kategori (kategori_itemcode). Urutan parent mengikuti key order. */
    private static final Map<String, List<String>> PARENT_CHILD_MAPPING = new LinkedHashMap<>();
    static {
        PARENT_CHILD_MAPPING.put("BRANDED", List.of("PCAIO", "PCBU", "PCMINI"));
        PARENT_CHILD_MAPPING.put("KOMPONEN", List.of("PROC", "MB", "VGA", "RAM", "SSD", "SSDEX", "HDIN3", "HDIN2", "HDEX3", "HDEX2", "CS", "PSU", "CLR", "FAN"));
        PARENT_CHILD_MAPPING.put("CTRD TINTA TONER", List.of("TINTA", "CARTD"));
        PARENT_CHILD_MAPPING.put("PRINTER SCANNER", List.of("PRINT", "SCAN"));
        PARENT_CHILD_MAPPING.put("ACC", List.of("ACS", "AL", "ATK", "BRIKT", "CCTV", "FP", "KAS", "KB", "KBL", "KBM", "MC", "MJ", "MM", "MS", "MSN", "PP", "SCR", "SOFT", "SP", "STAB", "UFD"));
    }

    /**
     * Ringkasan stok dua level: parent (nama, stok = jumlah dari children, presentase) dan children (sub kategori).
     * Kategori yang tidak ada di mapping tampil sebagai baris standalone. Baris terakhir TOTAL.
     */
    public List<StockSummaryRowResponse> getSummaryByCategoryHierarchy() {
        List<Object[]> rows = stockRepository.sumGrandTotalByKategoriItemcode();
        BigDecimal totalAll = stockRepository.sumAllGrandTotal();
        if (totalAll == null) totalAll = BigDecimal.ZERO;
        if (totalAll.compareTo(BigDecimal.ZERO) == 0) totalAll = BigDecimal.ONE;

        Map<String, BigDecimal> codeToStok = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String code = row[0] != null ? row[0].toString().trim() : null;
            BigDecimal stok = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            if (code != null && !code.isEmpty()) codeToStok.put(code, stok);
        }

        Set<String> codesUsedAsChild = PARENT_CHILD_MAPPING.values().stream()
                .flatMap(List::stream)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        List<StockSummaryRowResponse> result = new ArrayList<>();

        for (Map.Entry<String, List<String>> e : PARENT_CHILD_MAPPING.entrySet()) {
            String parentName = e.getKey();
            List<String> childCodes = e.getValue();
            List<StockSummaryRowResponse> children = new ArrayList<>();
            BigDecimal parentStok = BigDecimal.ZERO;
            for (String code : childCodes) {
                BigDecimal stok = codeToStok.get(code);
                if (stok == null) stok = codeToStok.get(code.toUpperCase());
                if (stok == null) stok = BigDecimal.ZERO;
                parentStok = parentStok.add(stok);
                BigDecimal presentase = stok.multiply(BigDecimal.valueOf(100)).divide(totalAll, 2, RoundingMode.HALF_UP);
                children.add(StockSummaryRowResponse.builder()
                        .nama(code)
                        .stok(stok)
                        .presentase(presentase)
                        .children(List.of())
                        .build());
            }
            BigDecimal parentPresentase = parentStok.multiply(BigDecimal.valueOf(100)).divide(totalAll, 2, RoundingMode.HALF_UP);
            result.add(StockSummaryRowResponse.builder()
                    .nama(parentName)
                    .stok(parentStok)
                    .presentase(parentPresentase)
                    .children(children)
                    .build());
        }

        for (Map.Entry<String, BigDecimal> e : codeToStok.entrySet()) {
            String code = e.getKey();
            if (codesUsedAsChild.contains(code.toUpperCase())) continue;
            BigDecimal stok = e.getValue();
            BigDecimal presentase = stok.multiply(BigDecimal.valueOf(100)).divide(totalAll, 2, RoundingMode.HALF_UP);
            result.add(StockSummaryRowResponse.builder()
                    .nama(code)
                    .stok(stok)
                    .presentase(presentase)
                    .children(List.of())
                    .build());
        }

        result.add(StockSummaryRowResponse.builder()
                .nama("TOTAL")
                .stok(totalAll)
                .presentase(new BigDecimal("100.00"))
                .children(List.of())
                .build());

        return result;
    }
}