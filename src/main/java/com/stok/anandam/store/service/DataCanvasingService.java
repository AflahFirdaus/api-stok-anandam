package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Canvasing;
import com.stok.anandam.store.core.postgres.model.DataCanvasing;
import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.repository.CanvasingRepository;
import com.stok.anandam.store.core.postgres.repository.DataCanvasingRepository;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.dto.DataCanvasingRequest;
import com.stok.anandam.store.exception.ResourceNotFoundException;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataCanvasingService {

    @Autowired
    private DataCanvasingRepository dataCanvasingRepository;

    @Autowired
    private CanvasingRepository canvasingRepository;

    @Autowired
    private UserRepository userRepository;

    // === 1. CREATE (Input Data Kunjungan) ===
    @Transactional
    public DataCanvasing create(DataCanvasingRequest request, String username) {
        // Cari Master Toko
        Canvasing canvasing = canvasingRepository.findById(request.getCanvasingId())
                .orElseThrow(() -> new ResourceNotFoundException("ID Canvasing " + request.getCanvasingId() + " tidak ditemukan"));

        // Cari User Pembuat
        User creator = userRepository.findByUsername(username)
                .orElse(null);

        // Buat Data Transaksi
        DataCanvasing data = DataCanvasing.builder()
                .canvasing(canvasing)
                .tanggal(request.getTanggal() != null ? request.getTanggal() : LocalDate.now())
                .canvasVisit(request.getCanvasVisit())
                .keterangan(request.getKeterangan())
                .catatan(request.getCatatan())
                .creator(creator)
                .creatorName(creator != null ? creator.getNama() : "System")
                .build();

        return dataCanvasingRepository.save(data);
    }

    // === 2. GET ALL (Filter Date, Search, Canvasing, Tipe Kunjungan) ===
    public Page<DataCanvasing> getAll(int page, int size, String sortBy, String dir,
                                      String startDateStr, String endDateStr, String search,
                                      Long canvasingId, String canvasVisit) {
        LocalDate start = (startDateStr != null && !startDateStr.isBlank()) ? LocalDate.parse(startDateStr) : LocalDate.of(2000, 1, 1);
        LocalDate end = (endDateStr != null && !endDateStr.isBlank()) ? LocalDate.parse(endDateStr) : LocalDate.now();

        Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DataCanvasing> result = dataCanvasingRepository.findByFilters(start, end, search, canvasingId, canvasVisit, pageable);
        if (result.getTotalPages() > 0 && page >= result.getTotalPages()) {
            pageable = PageRequest.of(0, size, Sort.by(direction, sortBy));
            result = dataCanvasingRepository.findByFilters(start, end, search, canvasingId, canvasVisit, pageable);
        }
        return result;
    }
}