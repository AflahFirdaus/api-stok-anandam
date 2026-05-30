package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.IjinImport;
import com.stok.anandam.store.core.postgres.repository.IjinImportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class IjinImportService {

    @Autowired
    private IjinImportRepository ijinImportRepository;

    /**
     * Ambil data ijin import dengan paging, sorting, dan search filter
     */
    public Page<IjinImport> getIjinImportData(int page, int size, String sortBy, String dir, String search) {
        
        // Atur arah sorting
        Sort.Direction direction = dir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        
        // Default fallback jika sortBy kosong
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "no"; 
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        if (search == null) {
            search = "";
        }

        return ijinImportRepository.findByFilters(search, pageable);
    }
}
