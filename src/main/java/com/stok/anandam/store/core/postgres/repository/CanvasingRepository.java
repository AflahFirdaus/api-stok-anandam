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

    // Cari berdasarkan Nama Instansi / Kabupaten / Kecamatan
    @Query("SELECT c FROM Canvasing c WHERE " +
        "(:search IS NULL OR :search = '' OR " +
        " LOWER(c.namaInstansi) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        " LOWER(c.kabupaten) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        " LOWER(c.kecamatan) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Canvasing> search(@Param("search") String search, Pageable pageable);
}