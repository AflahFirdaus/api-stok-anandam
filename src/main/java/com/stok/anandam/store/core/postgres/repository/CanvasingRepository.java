package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Canvasing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CanvasingRepository extends JpaRepository<Canvasing, Long> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE canvasing RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateTable();

    /** Search + filter opsional: search (namaInstansi, kabupaten, kecamatan), kategori, provinsi. */
    @Query("SELECT c FROM Canvasing c WHERE " +
        "(:search IS NULL OR :search = '' OR " +
        " LOWER(c.namaInstansi) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        " LOWER(COALESCE(c.kabupaten, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        " LOWER(COALESCE(c.kecamatan, '')) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
        "(:kategori IS NULL OR :kategori = '' OR LOWER(COALESCE(c.kategori, '')) LIKE LOWER(CONCAT('%', :kategori, '%'))) AND " +
        "(:provinsi IS NULL OR :provinsi = '' OR LOWER(COALESCE(c.provinsi, '')) LIKE LOWER(CONCAT('%', :provinsi, '%')))")
    Page<Canvasing> search(@Param("search") String search, @Param("kategori") String kategori, @Param("provinsi") String provinsi, Pageable pageable);
}