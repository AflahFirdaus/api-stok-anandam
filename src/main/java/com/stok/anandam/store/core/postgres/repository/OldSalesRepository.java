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
        java.util.List<String> findDistinctEmpCodeOrderByEmpCode();

        @Query("SELECT MAX(s.docDate) FROM OldSales s")
        java.util.Optional<LocalDate> findMaxDocDate();

        // ==================== LAPORAN OMSET PER MARKETING (OLD SALES) ====================

        /** Ringkasan per marketing: qty, omset, hpp, laba dari old_sales. */
        @Query("SELECT s.empCode, MIN(s.empName), " +
                        "COALESCE(SUM(s.qty), 0), COALESCE(SUM(s.grandTotal), 0), " +
                        "COALESCE(SUM(s.totalHpp), 0), COALESCE(SUM(s.labaKotor), 0) " +
                        "FROM OldSales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCodes IS NULL OR s.empCode IN :empCodes) " +
                        "GROUP BY s.empCode " +
                        "ORDER BY COALESCE(SUM(s.grandTotal), 0) DESC")
        java.util.List<Object[]> sumMarketingSalesByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCodes") java.util.List<String> empCodes);

        /** Detail drill-down per nota (doc) untuk satu marketing pada periode. */
        @Query("SELECT s.docNo, MAX(s.docDate), MAX(s.code), MAX(s.parName), " +
                        "COALESCE(SUM(s.qty), 0), COALESCE(SUM(s.grandTotal), 0), " +
                        "COALESCE(SUM(s.totalHpp), 0), COALESCE(SUM(s.labaKotor), 0), " +
                        "MAX(s.empCode), MAX(s.empName) " +
                        "FROM OldSales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCodes IS NULL OR s.empCode IN :empCodes) " +
                        "GROUP BY s.docNo " +
                        "ORDER BY MAX(s.docDate) DESC")
        java.util.List<Object[]> findNotaByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCodes") java.util.List<String> empCodes);

        /** Detail drill-down per barang (item) untuk satu marketing pada periode. */
        @Query("SELECT s.itemName, " +
                        "COALESCE(SUM(s.qty), 0), COALESCE(SUM(s.grandTotal), 0), " +
                        "COALESCE(SUM(s.totalHpp), 0), COALESCE(SUM(s.labaKotor), 0), " +
                        "MAX(s.empCode), MAX(s.empName) " +
                        "FROM OldSales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCodes IS NULL OR s.empCode IN :empCodes) " +
                        "GROUP BY s.itemName " +
                        "ORDER BY COALESCE(SUM(s.grandTotal), 0) DESC")
        java.util.List<Object[]> findItemByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCodes") java.util.List<String> empCodes);

        /** Ambil distinct kode marketing beserta namanya */
        @Query("SELECT DISTINCT s.empCode, s.empName FROM OldSales s WHERE s.empCode IS NOT NULL AND TRIM(s.empCode) <> '' ORDER BY s.empCode")
        java.util.List<Object[]> findDistinctEmpCodeAndName();
}
