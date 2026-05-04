package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.*;
import com.stok.anandam.store.core.postgres.repository.*;
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
import com.stok.anandam.store.core.postgres.model.Role;
import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

        @Autowired
        private PricelistRepository pricelistRepository;

        @Autowired
        private PurchaseRepository purchaseRepository;

        @Autowired
        private MemoItemRepository memoItemRepository;

        @Autowired
        private UserRepository userRepository;

        public Page<StockGroupedResponse> getGroupedStocks(int page, int size, String sortBy, String direction,
                        String search, String kategori, String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                Role role = user.getRole();
                boolean isMarketing = role == Role.MARKETING ||
                                role == Role.MARKETING_TOKO ||
                                role == Role.MARKETING_PROJECT ||
                                role == Role.MARKETING_DISTRIBUSI ||
                                role == Role.MARKETING_ONLINE;

                Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
                String actualSortBy = sortBy;
                // Map frontend sort names to backend field names if they differ
                if ("modalFinal".equals(sortBy))
                        actualSortBy = "p.modal";
                else if ("finalPricelist".equals(sortBy))
                        actualSortBy = "p.finalPricelist";
                else if ("spesifikasi".equals(sortBy))
                        actualSortBy = "p.spesifikasi";
                else if ("itemCode".equals(sortBy))
                        actualSortBy = "s.itemCode";
                else if ("itemName".equals(sortBy))
                        actualSortBy = "s.itemName";
                else if ("finalStok".equals(sortBy))
                        actualSortBy = "SUM(s.finalStok)";
                else
                        actualSortBy = "s.itemName";

                Pageable pageable = PageRequest.of(page, size); // Manual sorting in query if complex

                Page<String> itemCodePage;
                if (actualSortBy.startsWith("p.") || actualSortBy.startsWith("SUM") || actualSortBy.equals("s.itemName")) {
                        // Need special query for joined/agg sorting
                        itemCodePage = stockRepository.findDistinctItemCodesSortedByPricelist(
                                        search, kategori, actualSortBy, direction, pageable);
                } else {
                        itemCodePage = stockRepository.findDistinctItemCodes(search, kategori,
                                        PageRequest.of(page, size,
                                                        Sort.by(sortDirection, actualSortBy.replace("s.", ""))));
                }
                List<String> itemCodes = itemCodePage.getContent();

                if (itemCodes.isEmpty()) {
                        return Page.empty(pageable);
                }

                List<Stock> allStocks = stockRepository.findByItemCodeInAndFinalStokGreaterThanEqual(itemCodes, 1);

                // Fetch Last Sales Dates for these items
                List<String> itemNames = allStocks.stream()
                                .map(Stock::getItemName)
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .distinct()
                                .collect(Collectors.toList());

                Map<String, LocalDate> lastSalesDates = new HashMap<>();
                if (!itemNames.isEmpty()) {
                        List<Object[]> results = salesRepository.findLatestDocDatesByItemNames(itemNames);
                        for (Object[] res : results) {
                                String name = (String) res[0];
                                if (name != null) {
                                        lastSalesDates.put(name.trim(), (LocalDate) res[1]);
                                }
                        }
                }

                // Fetch Last Purchase Details (Date and Partner Name)
                Map<String, LocalDate> lastPurchaseDates = new HashMap<>();
                Map<String, String> lastPurchasePartners = new HashMap<>();
                if (!itemNames.isEmpty()) {
                        List<Object[]> results = purchaseRepository.findLatestPurchaseDetailsByItemNames(itemNames);
                        for (Object[] res : results) {
                                String name = (String) res[0];
                                LocalDate date = (LocalDate) res[1];
                                String partner = (String) res[2];

                                if (name != null) {
                                        String trimmedName = name.trim();
                                        lastPurchaseDates.put(trimmedName, date);
                                        lastPurchasePartners.put(trimmedName, partner);
                                }
                        }
                }

                Map<String, List<Stock>> groupedByCode = allStocks.stream()
                                .collect(Collectors.groupingBy(Stock::getItemCode));

                // Fetch pricelist data for all items in one go (Optimized: Fix N+1)
                List<String> normalizedNames = itemNames.stream()
                                .map(com.stok.anandam.store.util.NormalizationUtil::normalizeItemName)
                                .collect(Collectors.toList());
                
                Map<String, com.stok.anandam.store.core.postgres.model.Pricelist> pricelistMap = new HashMap<>();
                pricelistRepository.findByItemNameIn(normalizedNames).forEach(p -> {
                    // Map back to original name for lookup convenience in the stream below
                    // We need to find which original name matches this normalized one
                    itemNames.stream()
                        .filter(original -> com.stok.anandam.store.util.NormalizationUtil.normalizeItemName(original).equals(p.getItemName()))
                        .forEach(original -> pricelistMap.put(original, p));
                });

                List<StockGroupedResponse> groupedResponses = itemCodes.stream().map(code -> {
                        List<Stock> stocks = groupedByCode.get(code);
                        if (stocks == null || stocks.isEmpty()) return null;
                        
                        // Pick the best stock record to represent this code (priority to one matching search term)
                        Stock displayStock = stocks.get(0);
                        if (search != null && !search.isEmpty()) {
                            final String searchLower = search.toLowerCase();
                            displayStock = stocks.stream()
                                .filter(s -> s.getItemName() != null && s.getItemName().toLowerCase().contains(searchLower))
                                .findFirst()
                                .orElse(displayStock);
                        }
                        
                        String trimmedName = displayStock.getItemName() != null ? displayStock.getItemName().trim() : "";

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

                        com.stok.anandam.store.core.postgres.model.Pricelist priceInfo = pricelistMap
                                        .get(trimmedName);

                        StockGroupedResponse res = StockGroupedResponse.builder()
                                        .id(displayStock.getId())
                                        .itemCode(displayStock.getItemCode())
                                        .itemName(displayStock.getItemName())
                                        .kategoriNama(displayStock.getKategoriNama())
                                        .kategoriItemcode(displayStock.getKategoriItemcode())
                                        .totalStok(totalStok)
                                        .hargaHpp(displayStock.getHargaHpp())
                                        .grandTotal(grandTotal)
                                        .spesifikasi(priceInfo != null ? priceInfo.getSpesifikasi() : null)
                                        .modal(priceInfo != null ? priceInfo.getModal() : null)
                                        .finalPricelist(priceInfo != null ? priceInfo.getFinalPricelist() : null)
                                        .lastSalesDate(lastSalesDates.get(trimmedName))
                                        .lastPurchaseDate(lastPurchaseDates.get(trimmedName))
                                         .parName(lastPurchasePartners.get(trimmedName))
                                         .warehouses(warehouses)
                                         .totalPending(0) // Default
                                         .pendingDetails(new ArrayList<>()) // Default
                                         .build();

                        if (isMarketing) {
                            res.setHargaHpp(null);
                        }
                        return res;
                }).filter(Objects::nonNull).collect(Collectors.toList());

                // Fetch all Pending Items (Only Approved ones)
                List<MemoItem> allPendingItems = memoItemRepository.findByMemo_StatusAkhir(com.stok.anandam.store.core.postgres.model.enums.MemoStatus.DISETUJUI);
                
                // Group all pending items by normalized name
                Map<String, List<MemoItem>> pendingByName = allPendingItems.stream()
                        .collect(Collectors.groupingBy(item -> com.stok.anandam.store.util.NormalizationUtil.normalizeItemName(item.getNamaBarang())));

                // Enrich groupedResponses with pending info by matching normalized itemName
                for (StockGroupedResponse resp : groupedResponses) {
                    String respNormalized = com.stok.anandam.store.util.NormalizationUtil.normalizeItemName(resp.getItemName());
                    List<MemoItem> items = pendingByName.get(respNormalized);
                    if (items != null) {
                        int totalPending = items.stream().mapToInt(MemoItem::getQty).sum();
                        // Group by marketing name and sum quantities
                        Map<String, Integer> groupedDetails = items.stream()
                                .collect(Collectors.groupingBy(item -> {
                                    if (item.getMemo() == null) return "Unknown";
                                    if (item.getMemo().getMarketing() != null) {
                                        return item.getMemo().getMarketing().getNama();
                                    }
                                    if (item.getMemo().getMarketingName() != null && !item.getMemo().getMarketingName().isEmpty()) {
                                        return item.getMemo().getMarketingName();
                                    }
                                    return "Unknown";
                                }, Collectors.summingInt(MemoItem::getQty)));

                        List<com.stok.anandam.store.dto.PendingStockDTO> details = groupedDetails.entrySet().stream()
                                .map(entry -> com.stok.anandam.store.dto.PendingStockDTO.builder()
                                        .marketingNama(entry.getKey())
                                        .qty(entry.getValue())
                                        .build())
                                .collect(Collectors.toList());
                        
                        resp.setTotalPending(totalPending);
                        resp.setPendingDetails(details);
                    }
                }

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

        public Stock getSingleStockDetail(Long id, String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                Role role = user.getRole();
                boolean isMarketing = role == Role.MARKETING ||
                                role == Role.MARKETING_TOKO ||
                                role == Role.MARKETING_PROJECT ||
                                role == Role.MARKETING_DISTRIBUSI ||
                                role == Role.MARKETING_ONLINE;

                Stock stock = stockRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Stock not found with id: " + id));

                LocalDate lastSalesDate = salesRepository.findLatestDocDateByItemName(stock.getItemName());
                stock.setLastSalesDate(lastSalesDate);

                purchaseRepository.findLatestPurchaseByItemName(stock.getItemName()).ifPresent(p -> {
                        stock.setLastPurchaseDate(p.getDocDate());
                        stock.setParName(p.getParName());
                });

                // Fetch from pricelist using normalized name
                String normalizedName = com.stok.anandam.store.util.NormalizationUtil
                                .normalizeItemName(stock.getItemName());
                pricelistRepository.findByItemName(normalizedName).ifPresent(p -> {
                        stock.setSpesifikasi(p.getSpesifikasi());
                        stock.setModal(p.getModal());
                        stock.setFinalPricelist(p.getFinalPricelist());
                });

                if (isMarketing) {
                        stock.setHargaHpp(null);
                }

                return stock;
        }

        public Page<Stock> getAllStocks(int page, int size, String sortBy, String direction, String search) {
                Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

                return stockRepository.findByFilters(search, null, null, pageable);
        }
}