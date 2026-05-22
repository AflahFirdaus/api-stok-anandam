package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SalesRepository extends JpaRepository<Sales, Long> {

        @Modifying
        @Transactional
        @Query(value = "TRUNCATE TABLE sales RESTART IDENTITY", nativeQuery = true)
        void truncateTable();

        // QUERY 1: Ambil Data List dengan Filter Lengkap
        @Query("SELECT s FROM Sales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCode IS NULL OR :empCode = '' OR s.empCode = :empCode) AND " +
                        "(:categories IS NULL OR s.depCode IN :categories) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "((:searchColumn IS NULL OR :searchColumn = 'ALL' OR :searchColumn = '') AND (LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.depCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))) OR " +
                        "(:searchColumn = 'code' AND LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'noNota' AND LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'distributor' AND LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'barang' AND LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'dept' AND LOWER(s.depCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        ")")
        Page<Sales> findByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCode") String empCode,
                        @Param("categories") List<String> categories,
                        @Param("search") String search,
                        @Param("searchColumn") String searchColumn,
                        Pageable pageable);

        @Query("SELECT s FROM Sales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCode IS NULL OR :empCode = '' OR s.empCode = :empCode) AND " +
                        "(:categories IS NULL OR s.depCode IN :categories) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "((:searchColumn IS NULL OR :searchColumn = 'ALL' OR :searchColumn = '') AND (LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.depCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))) OR " +
                        "(:searchColumn = 'code' AND LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'noNota' AND LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'distributor' AND LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'barang' AND LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'dept' AND LOWER(s.depCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        ") ORDER BY s.docDate DESC")
        List<Sales> findAllByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCode") String empCode,
                        @Param("categories") List<String> categories,
                        @Param("search") String search,
                        @Param("searchColumn") String searchColumn);

        // QUERY 2: Hitung Total Grand (SUM) dengan filter yang sama
        @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCode IS NULL OR :empCode = '' OR s.empCode = :empCode) AND " +
                        "(:categories IS NULL OR s.depCode IN :categories) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "((:searchColumn IS NULL OR :searchColumn = 'ALL' OR :searchColumn = '') AND (LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.depCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))) OR " +
                        "(:searchColumn = 'code' AND LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'noNota' AND LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'distributor' AND LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'barang' AND LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'dept' AND LOWER(s.depCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        ")")
        BigDecimal sumGrandTotalByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCode") String empCode,
                        @Param("categories") List<String> categories,
                        @Param("search") String search,
                        @Param("searchColumn") String searchColumn);

        @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sales s WHERE s.docDate = :today")
        BigDecimal sumTotalByDate(@Param("today") LocalDate today);

        @Query("SELECT COALESCE(SUM(s.qty), 0) FROM Sales s WHERE " +
                        "(s.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:empCode IS NULL OR :empCode = '' OR s.empCode = :empCode) AND " +
                        "(:categories IS NULL OR s.depCode IN :categories) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "((:searchColumn IS NULL OR :searchColumn = 'ALL' OR :searchColumn = '') AND (LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.depCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))) OR " +
                        "(:searchColumn = 'code' AND LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'noNota' AND LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'distributor' AND LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'barang' AND LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                        "(:searchColumn = 'dept' AND LOWER(s.depCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        ")")
        Long sumQtyByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("empCode") String empCode,
                        @Param("categories") List<String> categories,
                        @Param("search") String search,
                        @Param("searchColumn") String searchColumn);

        @Query("SELECT MAX(s.docDate) FROM Sales s WHERE s.itemName = :itemName")
        LocalDate findLatestDocDateByItemName(@Param("itemName") String itemName);

        @Query("SELECT s.itemName, MAX(s.docDate) FROM Sales s WHERE s.itemName IN :itemNames GROUP BY s.itemName")
        List<Object[]> findLatestDocDatesByItemNames(@Param("itemNames") List<String> itemNames);

        @Query("SELECT s.empCode, MIN(s.empName), COALESCE(SUM(s.grandTotal), 0) FROM Sales s " +
                        "WHERE s.docDate = :today " +
                        "GROUP BY s.empCode " +
                        "ORDER BY SUM(s.grandTotal) DESC")
        List<Object[]> sumSalesByEmployeeToday(@Param("today") LocalDate today);

        @Query("SELECT s.empCode, MIN(s.empName), COALESCE(SUM(s.grandTotal), 0) FROM Sales s " +
                        "WHERE s.docDate BETWEEN :start AND :end " +
                        "GROUP BY s.empCode " +
                        "ORDER BY SUM(s.grandTotal) DESC")
        List<Object[]> sumSalesByEmployeeMonth(@Param("start") LocalDate start, @Param("end") LocalDate end);

        @Query("SELECT DISTINCT s.empCode, s.empName FROM Sales s WHERE s.empCode IS NOT NULL AND TRIM(s.empCode) <> '' ORDER BY s.empCode")
        java.util.List<Object[]> findDistinctEmpCodeAndName();

        @Query("SELECT DISTINCT s.empCode FROM Sales s WHERE s.empCode IS NOT NULL AND TRIM(s.empCode) <> '' ORDER BY s.empCode")
        java.util.List<String> findDistinctEmpCodeOrderByEmpCode();

        @Query("SELECT DISTINCT s.depCode FROM Sales s WHERE s.depCode IS NOT NULL AND TRIM(s.depCode) <> '' ORDER BY s.depCode")
        java.util.List<String> findDistinctDepCodes();
}