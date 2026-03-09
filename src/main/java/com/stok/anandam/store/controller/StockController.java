package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Stock;
import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.StockSummaryByCategoryResponse;
import com.stok.anandam.store.dto.StockSummaryRowResponse;
import com.stok.anandam.store.dto.StockGroupedResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.List;

@RestController
@RequestMapping(path = { "/api/v1/stock", "/api/v1/stocks" })
public class StockController {

        private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
                        "itemName", "itemCode", "kategoriNama", "kategoriItemcode", "finalStok", "grandTotal",
                        "warehouse", "id");

        @Autowired
        private StockService stockService;

        /**
         * GET list: filter search (nama/code), kategori (kategori_itemcode), warehouse.
         * Sort & paging.
         */
        @GetMapping
        public ResponseEntity<WebResponse<Page<StockGroupedResponse>>> getAllStocks(
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "10") int size,
                        @RequestParam(name = "sortBy", defaultValue = "itemName") String sortBy,
                        @RequestParam(name = "direction", defaultValue = "asc") String direction,
                        @RequestParam(name = "search", required = false) String search,
                        @RequestParam(name = "kategori", required = false) String kategori,
                        @RequestParam(name = "warehouse", required = false) String warehouse) {
                String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "itemName";

                Page<StockGroupedResponse> stocks = stockService.getGroupedStocks(page, size, safeSortBy, direction,
                                search, kategori);
                // Setelah search/filter, jika halaman yang diminta melebihi total halaman,
                // kembalikan halaman 0 agar list tidak kosong
                int totalPages = stocks.getTotalPages();
                if (totalPages > 0 && page >= totalPages) {
                        stocks = stockService.getGroupedStocks(0, size, safeSortBy, direction, search, kategori);
                        page = 0;
                }

                PagingResponse paging = PagingResponse.builder()
                                .currentPage(page)
                                .totalPage(stocks.getTotalPages())
                                .size(size)
                                .totalItem(stocks.getTotalElements())
                                .build();

                return ResponseEntity.ok(WebResponse.<Page<StockGroupedResponse>>builder()
                                .status(200)
                                .message("Success Fetch Stock Data")
                                .data(stocks)
                                .paging(paging)
                                .build());
        }

        /**
         * Ringkasan stok per kategori: NAMA, STOK (total grand_total), PRESENTASE (%).
         * Baris terakhir = TOTAL (100%).
         */
        @GetMapping("/summary-by-category")
        public ResponseEntity<WebResponse<List<StockSummaryByCategoryResponse>>> getSummaryByCategory(
                        @RequestParam(name = "groupBy", defaultValue = "kategori_itemcode") String groupBy) {
                List<StockSummaryByCategoryResponse> data = stockService.getSummaryByCategory(groupBy);
                return ResponseEntity.ok(WebResponse.<List<StockSummaryByCategoryResponse>>builder()
                                .status(200)
                                .message("Success fetch stock summary by category")
                                .data(data)
                                .paging(null)
                                .build());
        }

        /**
         * Ringkasan stok dua level: parent (stok = jumlah children) + children. Contoh:
         * BRANDED → PCAIO, PCBU, PCMINI.
         */
        @GetMapping("/summary-by-category/hierarchy")
        public ResponseEntity<WebResponse<List<StockSummaryRowResponse>>> getSummaryByCategoryHierarchy() {
                List<StockSummaryRowResponse> data = stockService.getSummaryByCategoryHierarchy();
                return ResponseEntity.ok(WebResponse.<List<StockSummaryRowResponse>>builder()
                                .status(200)
                                .message("Success fetch stock summary by category (hierarchy)")
                                .data(data)
                                .paging(null)
                                .build());
        }

        // FITUR 3: Detail Stok (Get By ID)
        @GetMapping("/{id}")
        public ResponseEntity<WebResponse<Stock>> getStockDetail(@PathVariable(name = "id") Long id) {
                Stock stock = stockService.getSingleStockDetail(id);

                return ResponseEntity.ok(WebResponse.<Stock>builder()
                                .status(200)
                                .message("Success Fetch Stock Detail")
                                .data(stock)
                                .paging(null)
                                .build());
        }
}