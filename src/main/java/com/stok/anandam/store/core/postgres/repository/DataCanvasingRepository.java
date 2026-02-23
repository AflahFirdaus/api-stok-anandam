package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.DataCanvasing;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DataCanvasingRepository extends JpaRepository<DataCanvasing, Long> {
    /** Filter: date range, search (nama instansi), canvasingId (toko), canvasVisit (Canvas/Visit). */
    @Query("SELECT d FROM DataCanvasing d WHERE " +
            "(d.tanggal BETWEEN :startDate AND :endDate) AND " +
            "(:search IS NULL OR :search = '' OR LOWER(d.canvasing.namaInstansi) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:canvasingId IS NULL OR d.canvasing.id = :canvasingId) AND " +
            "(:canvasVisit IS NULL OR :canvasVisit = '' OR LOWER(TRIM(COALESCE(d.canvasVisit, ''))) = LOWER(TRIM(:canvasVisit)))")
    Page<DataCanvasing> findByFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            @Param("canvasingId") Long canvasingId,
            @Param("canvasVisit") String canvasVisit,
            Pageable pageable
    );

    long countByTanggal(LocalDate today);
}