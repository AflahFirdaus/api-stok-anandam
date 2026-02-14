package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Stock;
import com.stok.anandam.store.core.postgres.repository.StockRepository;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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
}