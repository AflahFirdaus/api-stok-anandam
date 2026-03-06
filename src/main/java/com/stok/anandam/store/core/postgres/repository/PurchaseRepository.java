package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Purchase;

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
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

        // Fitur: Hapus semua data & Reset ID ke 1
        @Modifying
        @Transactional
        @Query(value = "TRUNCATE TABLE purchases RESTART IDENTITY", nativeQuery = true)
        void truncateTable();

        // Query 1: Ambil Data dengan Filter Tanggal & Pencarian
        @Query("SELECT p FROM Purchase p WHERE " +
                        "(p.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(p.docNoP) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.itemName) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<Purchase> findByDateRangeAndSearch(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT p FROM Purchase p WHERE " +
                        "(p.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(p.docNoP) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.itemName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "ORDER BY p.docDate DESC")
        java.util.List<Purchase> findAllByDateRangeAndSearch(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("search") String search);

        // Query 2: Hitung Total Uang (SUM Grand Total) dengan filter yang sama
        @Query("SELECT COALESCE(SUM(p.grandTotal), 0) FROM Purchase p WHERE " +
                        "(p.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(p.docNoP) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.itemName) LIKE LOWER(CONCAT('%', :search, '%')))")
        BigDecimal sumGrandTotalByDateRange(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("search") String search);

        @Query("SELECT COALESCE(SUM(p.grandTotal), 0) FROM Purchase p WHERE p.docDate = :today")
        BigDecimal sumTotalByDate(@Param("today") LocalDate today);

        @Query("SELECT p.itemName, p.docDate, p.parName FROM Purchase p WHERE p.itemName IN :itemNames AND p.docDate = (SELECT MAX(p2.docDate) FROM Purchase p2 WHERE p2.itemName = p.itemName)")
        List<Object[]> findLatestPurchaseDetailsByItemNames(@Param("itemNames") List<String> itemNames);

        @Query(value = "SELECT * FROM purchases WHERE item_name = :itemName ORDER BY doc_date DESC LIMIT 1", nativeQuery = true)
        java.util.Optional<Purchase> findLatestPurchaseByItemName(@Param("itemName") String itemName);
}