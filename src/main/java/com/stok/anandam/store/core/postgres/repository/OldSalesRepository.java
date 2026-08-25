package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.OldSales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OldSalesRepository extends JpaRepository<OldSales, Long> {

        @Query("SELECT s FROM OldSales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCode IS NULL OR :empCode = '' OR s.empCode = :empCode) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<OldSales> findByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCode") String empCode,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM OldSales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCode IS NULL OR :empCode = '' OR s.empCode = :empCode) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')))")
        BigDecimal sumGrandTotalByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCode") String empCode,
                        @Param("search") String search);

        @Query("SELECT COALESCE(SUM(s.qty), 0) FROM OldSales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCode IS NULL OR :empCode = '' OR s.empCode = :empCode) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')))")
        BigDecimal sumQtyByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCode") String empCode,
                        @Param("search") String search);

        @Query("SELECT DISTINCT s.empCode FROM OldSales s WHERE s.empCode IS NOT NULL AND TRIM(s.empCode) <> '' ORDER BY s.empCode")
        List<String> findDistinctEmpCodeOrderByEmpCode();

        @Query("SELECT MAX(s.docDate) FROM OldSales s")
        java.util.Optional<LocalDate> findMaxDocDate();

        // ==================== LAPORAN OMSET PER MARKETING (OLD SALES) ====================

        /** Ringkasan per marketing: qty, omset, hpp, laba dari old_sales.
         *  Normalisasi: spasi pada empCode lama diganti '_' agar cocok dengan kode baru.
         *  Contoh: 'MKT PROJECT' → 'MKT_PROJECT' */
        @Query("SELECT REPLACE(UPPER(TRIM(s.empCode)), ' ', '_'), MIN(s.empName), " +
                        "COALESCE(SUM(s.qty), 0), COALESCE(SUM(s.grandTotal), 0), " +
                        "COALESCE(SUM(s.totalHpp), 0), COALESCE(SUM(s.labaKotor), 0) " +
                        "FROM OldSales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCodes IS NULL OR REPLACE(UPPER(TRIM(s.empCode)), ' ', '_') IN :empCodes) " +
                        "GROUP BY REPLACE(UPPER(TRIM(s.empCode)), ' ', '_') " +
                        "ORDER BY COALESCE(SUM(s.grandTotal), 0) DESC")
        List<Object[]> sumMarketingSalesByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCodes") List<String> empCodes);

        /** Detail drill-down per nota (doc) untuk satu marketing pada periode. */
        @Query("SELECT s.docNo, s.docDate, MAX(s.code), MAX(s.parName), " +
                        "COALESCE(SUM(s.qty), 0), COALESCE(SUM(s.grandTotal), 0), " +
                        "COALESCE(SUM(s.totalHpp), 0), COALESCE(SUM(s.labaKotor), 0), " +
                        "MAX(REPLACE(UPPER(TRIM(s.empCode)), ' ', '_')), MAX(s.empName) " +
                        "FROM OldSales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCodes IS NULL OR REPLACE(UPPER(TRIM(s.empCode)), ' ', '_') IN :empCodes) " +
                        "GROUP BY s.docNo, s.docDate " +
                        "ORDER BY s.docDate DESC")
        List<Object[]> findNotaByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCodes") List<String> empCodes);

        /** Detail drill-down per barang (item) untuk satu marketing pada periode. */
        @Query("SELECT s.itemName, " +
                        "COALESCE(SUM(s.qty), 0), COALESCE(SUM(s.grandTotal), 0), " +
                        "COALESCE(SUM(s.totalHpp), 0), COALESCE(SUM(s.labaKotor), 0), " +
                        "MAX(REPLACE(UPPER(TRIM(s.empCode)), ' ', '_')), MAX(s.empName) " +
                        "FROM OldSales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCodes IS NULL OR REPLACE(UPPER(TRIM(s.empCode)), ' ', '_') IN :empCodes) " +
                        "GROUP BY s.itemName " +
                        "ORDER BY COALESCE(SUM(s.grandTotal), 0) DESC")
        List<Object[]> findItemByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCodes") List<String> empCodes);

        /** Ambil distinct kode marketing beserta namanya — dinormalisasi spasi→'_' */
        @Query("SELECT DISTINCT REPLACE(UPPER(TRIM(s.empCode)), ' ', '_'), MAX(s.empName) " +
                        "FROM OldSales s WHERE s.empCode IS NOT NULL AND TRIM(s.empCode) <> '' " +
                        "GROUP BY REPLACE(UPPER(TRIM(s.empCode)), ' ', '_') ORDER BY REPLACE(UPPER(TRIM(s.empCode)), ' ', '_')")
        List<Object[]> findDistinctEmpCodeAndName();
}
