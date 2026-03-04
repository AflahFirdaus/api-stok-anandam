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
}
