package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Tkdn;
import com.stok.anandam.store.core.postgres.repository.TkdnRepository;
import com.stok.anandam.store.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TkdnService {

    @Autowired
    private TkdnRepository tkdnRepository;

    public Page<Tkdn> getAllTkdn(int page, int size, String sortBy, String dir,
            Boolean isTkdn, String kategori, String search,
            String processor, String ram, String ssd, String hdd, String vga, String layar, String os) {

        Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        String searchPattern = toPattern(search);
        String procPattern = toPattern(processor);
        String ramPattern = toPattern(ram);
        String ssdPattern = toPattern(ssd);
        String hddPattern = toPattern(hdd);
        String vgaPattern = toPattern(vga);
        String layarPattern = toPattern(layar);
        String osPattern = toPattern(os);

        boolean hasSpecFilter = hasValue(processor) || hasValue(ram) || hasValue(ssd) || hasValue(hdd)
                || hasValue(vga) || hasValue(layar) || hasValue(os);

        if (hasSpecFilter) {
            return tkdnRepository.findByFiltersWithSpec(
                    isTkdn, kategori, searchPattern, procPattern, ramPattern, ssdPattern, hddPattern, vgaPattern,
                    layarPattern, osPattern, pageable);
        }
        return tkdnRepository.findByFilters(isTkdn, kategori, searchPattern, pageable);
    }

    private String toPattern(String s) {
        if (s == null || s.isBlank())
            return null;
        return "%" + s.trim() + "%";
    }

    private static boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    public java.util.List<String> getCategories() {
        return tkdnRepository.findDistinctKategoriOrderByKategori();
    }

    public Tkdn getTkdnById(Integer id) {
        return tkdnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Data TKDN ID " + id + " tidak ditemukan"));
    }
}