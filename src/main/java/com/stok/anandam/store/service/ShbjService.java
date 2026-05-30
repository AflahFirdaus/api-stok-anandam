package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Shbj;
import com.stok.anandam.store.core.postgres.repository.ShbjRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShbjService {

    @Autowired
    private ShbjRepository shbjRepository;

    /**
     * Get paginated data with optional search filter
     */
    public Page<Shbj> getShbjData(int page, int size, String sortBy, String dir, String search) {
        
        // Atur arah sorting (ASC / DESC)
        Sort.Direction direction = dir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        
        // Buat Pageable object (Catatan: perhatikan default fallback jika sortBy kosong)
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "id"; 
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        if (search == null) {
            search = "";
        }

        // Panggil repository
        return shbjRepository.findByFilters(search, pageable);
    }

    /**
     * Dapatkan daftar kategori unik untuk dropdown
     */
    public List<String> getKategoriList() {
        return shbjRepository.findDistinctUraianKelompokBarang();
    }
}