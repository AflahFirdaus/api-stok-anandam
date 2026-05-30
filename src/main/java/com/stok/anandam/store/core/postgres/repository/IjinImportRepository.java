package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.IjinImport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IjinImportRepository extends JpaRepository<IjinImport, Integer> {

    /**
     * Filter opsional: search (mencari di nama_barang, spesifikasi, atau keterangan)
     */
    @Query("SELECT i FROM IjinImport i WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(i.namaBarang) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.spesifikasi) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.keterangan) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<IjinImport> findByFilters(@Param("search") String search, Pageable pageable);
}
