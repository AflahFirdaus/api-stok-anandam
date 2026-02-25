package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Stock;
import com.stok.anandam.store.core.postgres.repository.StockRepository;
import com.stok.anandam.store.core.postgres.repository.SalesRepository;
import com.stok.anandam.store.dto.StockSummaryByCategoryResponse;
import com.stok.anandam.store.dto.StockSummaryRowResponse;
import com.stok.anandam.store.dto.StockGroupedResponse;
import com.stok.anandam.store.dto.WarehouseStockDTO;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockService {

        @Autowired
        private StockRepository stockRepository;

        @Autowired
        private SalesRepository salesRepository;

        public Page<StockGroupedResponse> getGroupedStocks(int page, int size, String sortBy, String direction,
                        String search, String kategori) {
                Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
                String actualSortBy = "itemCode".equals(sortBy) || "itemName".equals(sortBy) ? sortBy : "itemCode";
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, actualSortBy));

                Page<String> itemCodePage = stockRepository.findDistinctItemCodes(search, kategori, pageable);
                List<String> itemCodes = itemCodePage.getContent();

                if (itemCodes.isEmpty()) {
                        return Page.empty(pageable);
                }

                List<Stock> allStocks = stockRepository.findByItemCodeInAndFinalStokGreaterThanEqual(itemCodes, 1);

                // Fetch Last Sales Dates for these items
                List<String> itemNames = allStocks.stream().map(Stock::getItemName).distinct()
                                .collect(Collectors.toList());
                Map<String, LocalDate> lastSalesDates = new HashMap<>();
                if (!itemNames.isEmpty()) {
                        List<Object[]> results = salesRepository.findLatestDocDatesByItemNames(itemNames);
                        for (Object[] res : results) {
                                lastSalesDates.put((String) res[0], (LocalDate) res[1]);
                        }
                }

                Map<String, List<Stock>> groupedByCode = allStocks.stream()
                                .collect(Collectors.groupingBy(Stock::getItemCode));

                List<StockGroupedResponse> groupedResponses = itemCodes.stream().map(code -> {
                        List<Stock> stocks = groupedByCode.get(code);
                        Stock first = stocks.get(0);

                        List<WarehouseStockDTO> warehouses = stocks.stream()
                                        .map(s -> WarehouseStockDTO.builder()
                                                        .warehouse(s.getWarehouse())
                                                        .stok(s.getFinalStok())
                                                        .build())
                                        .collect(Collectors.toList());

                        int totalStok = stocks.stream().mapToInt(Stock::getFinalStok).sum();
                        BigDecimal grandTotal = stocks.stream()
                                        .map(Stock::getGrandTotal)
                                        .filter(Objects::nonNull)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        return StockGroupedResponse.builder()
                                        .id(first.getId())
                                        .itemCode(first.getItemCode())
                                        .itemName(first.getItemName())
                                        .kategoriNama(first.getKategoriNama())
                                        .kategoriItemcode(first.getKategoriItemcode())
                                        .totalStok(totalStok)
                                        .hargaHpp(first.getHargaHpp())
                                        .grandTotal(grandTotal)
                                        .spesifikasi(first.getSpesifikasi())
                                        .modal(first.getModal())
                                        .finalPricelist(first.getFinalPricelist())
                                        .lastSalesDate(lastSalesDates.get(first.getItemName()))
                                        .warehouses(warehouses)
                                        .build();
                }).collect(Collectors.toList());

                return new org.springframework.data.domain.PageImpl<>(groupedResponses, pageable,
                                itemCodePage.getTotalElements());
        }

        public List<StockSummaryByCategoryResponse> getSummaryByCategory(String groupBy) {
                List<Object[]> results;
                if ("kategori_nama".equalsIgnoreCase(groupBy)) {
                        results = stockRepository.sumGrandTotalByKategoriNama();
                } else {
                        results = stockRepository.sumGrandTotalByKategoriItemcode();
                }

                BigDecimal totalAll = stockRepository.sumAllGrandTotal();
                List<StockSummaryByCategoryResponse> data = new ArrayList<>();

                for (Object[] res : results) {
                        String name = (String) res[0];
                        BigDecimal grandTotal = (BigDecimal) res[1];
                        BigDecimal percentage = totalAll.compareTo(BigDecimal.ZERO) > 0
                                        ? grandTotal.multiply(new BigDecimal(100)).divide(totalAll, 2,
                                                        RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO;

                        data.add(StockSummaryByCategoryResponse.builder()
                                        .nama(name)
                                        .stok(grandTotal)
                                        .presentase(percentage)
                                        .build());
                }

                return data;
        }

        public List<StockSummaryRowResponse> getSummaryByCategoryHierarchy() {
                List<Object[]> rawResults = stockRepository.sumGrandTotalByKategoriHierarchy();
                BigDecimal totalAll = stockRepository.sumAllGrandTotal();

                // 1. Define the manual order and mapping based on the image
                // key: Parent Name (from image), value: List of sub-category prefixes/codes
                // from DB
                Map<String, List<String>> manualHierarchy = new LinkedHashMap<>();
                manualHierarchy.put("2ND", List.of("2ND"));
                manualHierarchy.put("PROJEKTOR", List.of("PROJEKTOR", "PROJ", "PROJECTOR", "PJT"));
                manualHierarchy.put("UPS", List.of("UPS"));
                manualHierarchy.put("BRANDED", List.of("PCAIO", "PCBU", "PCMINI"));
                manualHierarchy.put("NETWORK", List.of("NETWORK", "NET", "NWK"));
                manualHierarchy.put("NOTEBOOK", List.of("NOTEBOOK", "NB"));
                manualHierarchy.put("MONITOR", List.of("MONITOR", "MON", "LCD"));
                manualHierarchy.put("KOMPONEN", List.of("PROC", "MB", "VGA", "RAM", "SSD", "SSDEX", "HDIN3", "HDIN2",
                                "HDEX3", "HDEX2", "CS", "PSU", "CLR", "FAN"));
                manualHierarchy.put("CTRD TINTA TONER", List.of("TINTA", "CARTD"));
                manualHierarchy.put("PRINTER SCANNER", List.of("PRINT", "SCAN"));
                manualHierarchy.put("ACC", List.of("ACS", "AL", "ATK", "BRKT", "CCTV", "FP", "KAS", "KB", "KBL", "KBM",
                                "MC", "MI", "MM", "MS", "MSN", "PP", "SCR", "SOFT", "SP", "STAB", "UFD"));
                manualHierarchy.put("HPTB", List.of("HPTB", "HP", "TAB"));

                // 2. Prepare buckets for aggregation
                Map<String, StockSummaryRowResponse> rowMap = new LinkedHashMap<>();
                for (String parentName : manualHierarchy.keySet()) {
                        rowMap.put(parentName, StockSummaryRowResponse.builder()
                                        .nama(parentName)
                                        .stok(BigDecimal.ZERO)
                                        .presentase(BigDecimal.ZERO)
                                        .children(new ArrayList<>())
                                        .build());
                }

                // 3. Populate buckets from raw data
                for (Object[] res : rawResults) {
                        String kategoriItemcode = (String) res[0];
                        // String kategoriNama = (String) res[1];
                        BigDecimal grandTotalNode = (BigDecimal) res[2];

                        // Match kategoriItemcode to our manualHierarchy
                        String targetParent = null;
                        if (kategoriItemcode != null) {
                                final String normalizedCode = kategoriItemcode.trim().toUpperCase();
                                for (Map.Entry<String, List<String>> entry : manualHierarchy.entrySet()) {
                                        if (entry.getValue().stream()
                                                        .anyMatch(code -> code.equalsIgnoreCase(normalizedCode))) {
                                                targetParent = entry.getKey();
                                                break;
                                        }
                                }
                        }

                        if (targetParent != null) {
                                StockSummaryRowResponse parent = rowMap.get(targetParent);
                                parent.setStok(parent.getStok().add(grandTotalNode));

                                // If the parent is the same as the child (e.g. 2ND, PROJEKTOR, UPS, etc.), we
                                // don't necessarily need children
                                // unless it is one of those with multiple codes.
                                // But based on the image, even single categories can be shown.
                                // Image shows 2ND, PROJEKTOR, UPS as main rows without sub-items below them if
                                // they only have one.
                                // However, they exhibit specialized behavior: BRANDED has PCAIO, PCBU, PCMINI
                                // INDENTED.

                                List<String> subCategoryCodes = manualHierarchy.get(targetParent);
                                if (subCategoryCodes.size() > 1 || (subCategoryCodes.size() == 1
                                                && !subCategoryCodes.get(0).equals(targetParent))) {
                                        // Add as a child if it's part of a group
                                        BigDecimal childPercentage = totalAll.compareTo(BigDecimal.ZERO) > 0
                                                        ? grandTotalNode.multiply(new BigDecimal(100)).divide(totalAll,
                                                                        2, RoundingMode.HALF_UP)
                                                        : BigDecimal.ZERO;

                                        parent.getChildren().add(StockSummaryRowResponse.builder()
                                                        .nama(kategoriItemcode) // Use the itemcode as label (matches
                                                                                // labels like PROC, MB, VGA in image)
                                                        .stok(grandTotalNode)
                                                        .presentase(childPercentage)
                                                        .children(new ArrayList<>())
                                                        .build());
                                }
                        }
                }

                // 4. Finalize Parent Percentages and return list
                List<StockSummaryRowResponse> finalData = new ArrayList<>();
                for (StockSummaryRowResponse parent : rowMap.values()) {
                        BigDecimal parentPercentage = totalAll.compareTo(BigDecimal.ZERO) > 0
                                        ? parent.getStok().multiply(new BigDecimal(100)).divide(totalAll, 2,
                                                        RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO;
                        parent.setPresentase(parentPercentage);
                        finalData.add(parent);
                }

                // 5. Add TOTAL row
                finalData.add(StockSummaryRowResponse.builder()
                                .nama("TOTAL")
                                .stok(totalAll)
                                .presentase(new BigDecimal(100))
                                .children(new ArrayList<>())
                                .build());

                return finalData;
        }

        public Stock getSingleStockDetail(Long id) {
                Stock stock = stockRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Stock not found with id: " + id));

                LocalDate lastSalesDate = salesRepository.findLatestDocDateByItemName(stock.getItemName());
                stock.setLastSalesDate(lastSalesDate);
                return stock;
        }

        public Page<Stock> getAllStocks(int page, int size, String sortBy, String direction, String search) {
                Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

                return stockRepository.findByFilters(search, null, null, pageable);
        }
}