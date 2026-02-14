package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Sales;

import java.math.BigDecimal;
import java.time.LocalDate;

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
           "(:empCode IS NULL OR :empCode = '' OR s.empCode = :empCode) AND " + // Filter Employee
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(s.docNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Sales> findByFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("empCode") String empCode,
            @Param("search") String search,
            Pageable pageable
    );

    // QUERY 2: Hitung Total Grand (SUM) dengan filter yang sama
    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sales s WHERE " +
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
            @Param("search") String search
    );

    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sales s WHERE s.docDate = :today")
            BigDecimal sumTotalByDate(@Param("today") LocalDate today);
}