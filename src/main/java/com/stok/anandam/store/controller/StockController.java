package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Stock;
import com.stok.anandam.store.core.postgres.repository.StockRepository;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = { "/api/v1/stock", "/api/v1/stocks" })
public class StockController {

    @Autowired
    private StockRepository stockRepository;

    // FITUR 1 & 2: Get All + Search (Find Stok / Filter by Code)
    @GetMapping
    public ResponseEntity<WebResponse<Page<Stock>>> getAllStocks(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "itemName") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam(name = "search", required = false) String search // <--- Parameter Tambahan
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<Stock> stocks;

        // Logika Search
        if (search != null && !search.isBlank()) {
            // Cari di Nama atau Kode Barang
            stocks = stockRepository.findByItemNameContainingIgnoreCaseOrItemCodeContainingIgnoreCase(search, search, pageable);
        } else {
            // Ambil Semua
            stocks = stockRepository.findAll(pageable);
        }

        return ResponseEntity.ok(WebResponse.<Page<Stock>>builder()
                .status(200)
                .message("Success Fetch Stock Data")
                .data(stocks)
                .build());
    }

    // FITUR 3: Detail Stok (Get By ID)
    @GetMapping("/{id}")
    public ResponseEntity<WebResponse<Stock>> getStockDetail(@PathVariable Long id) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found with id: " + id));

        return ResponseEntity.ok(WebResponse.<Stock>builder()
                .status(200)
                .message("Success Fetch Stock Detail")
                .data(stock)
                .build());
    }
}