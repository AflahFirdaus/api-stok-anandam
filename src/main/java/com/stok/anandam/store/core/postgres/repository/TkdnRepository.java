package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Tkdn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TkdnRepository extends JpaRepository<Tkdn, Integer> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE tkdn RESTART IDENTITY", nativeQuery = true)
    void truncateTable();

    // Fitur: Cari berdasarkan Nama + Pagination otomatis
    Page<Tkdn> findByNamaContainingIgnoreCase(String nama, Pageable pageable);
    
    // Fitur: Filter berdasarkan Kategori + Pagination
    Page<Tkdn> findByKategoriIgnoreCase(String kategori, Pageable pageable);

    @Query("SELECT t FROM Tkdn t WHERE " +
            // 1. LOGIC FILTER TKDN (Sertifikat tidak boleh kosong)
            "(:isTkdn IS NULL OR " +
            "  (:isTkdn = true AND t.sertifikatTkd IS NOT NULL AND LENGTH(TRIM(t.sertifikatTkd)) > 0) OR " +
            "  (:isTkdn = false AND (t.sertifikatTkd IS NULL OR LENGTH(TRIM(t.sertifikatTkd)) = 0))) " +

            "AND " +

            // 2. LOGIC FILTER KATEGORI (Sempat hilang, sekarang dikembalikan)
            "(:kategori IS NULL OR :kategori = '' OR LOWER(t.kategori) = LOWER(:kategori)) " +

            "AND " +

            // 3. LOGIC SEARCH (Nama / No Merek / Kategori)
            "(:search IS NULL OR :search = '' OR " +
            "  LOWER(t.nama) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "  LOWER(t.noMerek) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "  LOWER(t.kategori) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<Tkdn> findByFilters(
            @Param("isTkdn") Boolean isTkdn,
            @Param("kategori") String kategori, // <--- INI SUDAH ADA LAGI
            @Param("search") String search,
            Pageable pageable
    );
}