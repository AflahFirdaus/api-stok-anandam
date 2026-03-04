package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.OldPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface OldPurchaseRepository extends JpaRepository<OldPurchase, Long> {

        @Query("SELECT p FROM OldPurchase p WHERE " +
                        "(p.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(p.docNoP) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.itemName) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<OldPurchase> findByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT COALESCE(SUM(p.grandTotal), 0) FROM OldPurchase p WHERE " +
                        "(p.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(p.docNoP) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.itemName) LIKE LOWER(CONCAT('%', :search, '%')))")
        BigDecimal sumGrandTotalByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("search") String search);

        @Query("SELECT COALESCE(SUM(p.qty), 0) FROM OldPurchase p WHERE " +
                        "(p.docDate BETWEEN :startDate AND :endDate) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(p.docNoP) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.parName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.itemName) LIKE LOWER(CONCAT('%', :search, '%')))")
        BigDecimal sumQtyByFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("search") String search);

        @Query("SELECT MAX(p.docDate) FROM OldPurchase p")
        java.util.Optional<LocalDate> findMaxDocDate();
}
