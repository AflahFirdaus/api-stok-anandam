package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Shbj;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShbjRepository extends JpaRepository<Shbj, Long> {

    /**
     * Filter opsional: search (mencari di uraian_barang ATAU spesifikasi).
     * Jika search kosong, akan mengembalikan semua data.
     */
    @Query("SELECT s FROM Shbj s WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(s.uraianBarang) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.spesifikasi) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Shbj> findByFilters(@Param("search") String search, Pageable pageable);

    // Endpoint opsional jika butuh list semua kategori unik untuk dropdown frontend
    @Query("SELECT DISTINCT s.uraianKelompokBarang FROM Shbj s ORDER BY s.uraianKelompokBarang ASC")
    List<String> findDistinctUraianKelompokBarang();
}