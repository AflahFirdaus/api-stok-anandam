package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Stock;

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
public interface StockRepository extends JpaRepository<Stock, Long> {

        @Modifying
        @Transactional
        @Query(value = "TRUNCATE TABLE stok RESTART IDENTITY", nativeQuery = true)
        void truncateTable();

        // Cari berdasarkan Item Code (Exact match atau Partial?)
        // Kita pakai partial biar gampang:
        // Cari berdasarkan Nama Item (Case Insensitive)
        Page<Stock> findByItemNameContainingIgnoreCase(String itemName, Pageable pageable);

        // Cari berdasarkan Item Code (Case Insensitive)
        Page<Stock> findByItemCodeContainingIgnoreCase(String itemCode, Pageable pageable);

        // Cari Global (Bisa ketik Kode atau Nama di satu kolom search)
        Page<Stock> findByItemNameContainingIgnoreCaseOrItemCodeContainingIgnoreCase(String itemName, String itemCode,
                        Pageable pageable);

        /**
         * Filter opsional: search (nama/code), kategori (kategori_itemcode), warehouse.
         * Semua AND, contains/equals ignore case.
         */
        @Query("SELECT s FROM Stock s WHERE " +
                        "(:search IS NULL OR :search = '' OR LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.itemCode) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                        +
                        "(:kategori IS NULL OR :kategori = '' OR LOWER(TRIM(COALESCE(s.kategoriItemcode, ''))) = LOWER(TRIM(:kategori)) OR LOWER(s.kategoriItemcode) LIKE LOWER(CONCAT('%', :kategori, '%'))) AND "
                        +
                        "(:warehouse IS NULL OR :warehouse = '' OR LOWER(COALESCE(s.warehouse, '')) LIKE LOWER(CONCAT('%', :warehouse, '%'))) AND "
                        +
                        "s.finalStok >= 1")
        Page<Stock> findByFilters(@Param("search") String search, @Param("kategori") String kategori,
                        @Param("warehouse") String warehouse, Pageable pageable);

        /** Fetch unique item codes with filtering. */
        @Query("SELECT s.itemCode FROM Stock s WHERE " +
                        "(:search IS NULL OR :search = '' OR LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.itemCode) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                        +
                        "(:kategori IS NULL OR :kategori = '' OR LOWER(TRIM(COALESCE(s.kategoriItemcode, ''))) = LOWER(TRIM(:kategori)) OR LOWER(s.kategoriItemcode) LIKE LOWER(CONCAT('%', :kategori, '%'))) AND "
                        +
                        "s.finalStok >= 1 " +
                        "GROUP BY s.itemCode, s.itemName")
        Page<String> findDistinctItemCodes(@Param("search") String search, @Param("kategori") String kategori,
                        Pageable pageable);

        /** Fetch unique item codes with joined sorting. */
        @Query("SELECT s.itemCode FROM Stock s LEFT JOIN Pricelist p ON s.normalizedItemName = p.itemName WHERE " +
                        "(:search IS NULL OR :search = '' OR LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.itemCode) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                        +
                        "(:kategori IS NULL OR :kategori = '' OR LOWER(TRIM(COALESCE(s.kategoriItemcode, ''))) = LOWER(TRIM(:kategori)) OR LOWER(s.kategoriItemcode) LIKE LOWER(CONCAT('%', :kategori, '%'))) AND "
                        +
                        "s.finalStok >= 1 " +
                        "GROUP BY s.itemCode, s.itemName, p.modal, p.finalPricelist, p.spesifikasi " +
                        "ORDER BY " +
                        "CASE WHEN :direction = 'asc' AND :sortBy = 'p.modal' THEN p.modal END ASC, " +
                        "CASE WHEN :direction = 'asc' AND :sortBy = 'p.finalPricelist' THEN p.finalPricelist END ASC, "
                        +
                        "CASE WHEN :direction = 'asc' AND :sortBy = 'p.spesifikasi' THEN p.spesifikasi END ASC, " +
                        "CASE WHEN :direction = 'asc' AND :sortBy = 'SUM(s.finalStok)' THEN SUM(s.finalStok) END ASC, "
                        +
                        "CASE WHEN :direction = 'desc' AND :sortBy = 'p.modal' THEN p.modal END DESC, " +
                        "CASE WHEN :direction = 'desc' AND :sortBy = 'p.finalPricelist' THEN p.finalPricelist END DESC, "
                        +
                        "CASE WHEN :direction = 'desc' AND :sortBy = 'p.spesifikasi' THEN p.spesifikasi END DESC, " +
                        "CASE WHEN :direction = 'desc' AND :sortBy = 'SUM(s.finalStok)' THEN SUM(s.finalStok) END DESC")
        Page<String> findDistinctItemCodesSortedByPricelist(@Param("search") String search,
                        @Param("kategori") String kategori,
                        @Param("sortBy") String sortBy,
                        @Param("direction") String direction,
                        Pageable pageable);

        /**
         * Fetch all stock records for a list of item codes, respecting finalStok >= 1.
         */
        List<Stock> findByItemCodeInAndFinalStokGreaterThanEqual(List<String> itemCodes, Integer minStok);

        @Query("SELECT COUNT(s) FROM Stock s WHERE s.finalStok < :threshold AND s.finalStok >= 1")
        long countByFinalStokLessThan(@Param("threshold") Integer threshold);

        @Query("SELECT s FROM Stock s WHERE s.finalStok < :threshold AND s.finalStok >= 1 ORDER BY s.finalStok ASC LIMIT 5")
        List<Stock> findTop5ByLowStock(@Param("threshold") Integer threshold);

        /**
         * Agregasi: group by kategori_itemcode, sum(grand_total). Urut nilai terbesar
         * dulu.
         */
        @Query("SELECT COALESCE(NULLIF(TRIM(s.kategoriItemcode), ''), 'LAIN-LAIN'), COALESCE(SUM(s.grandTotal), 0) FROM Stock s WHERE s.finalStok >= 1 GROUP BY COALESCE(NULLIF(TRIM(s.kategoriItemcode), ''), 'LAIN-LAIN') ORDER BY COALESCE(SUM(s.grandTotal), 0) DESC")
        List<Object[]> sumGrandTotalByKategoriItemcode();

        /**
         * Agregasi: group by kategori_nama, sum(grand_total). Urut nilai terbesar dulu.
         */
        @Query("SELECT COALESCE(NULLIF(TRIM(s.kategoriNama), ''), 'LAIN-LAIN'), COALESCE(SUM(s.grandTotal), 0) FROM Stock s WHERE s.finalStok >= 1 GROUP BY COALESCE(NULLIF(TRIM(s.kategoriNama), ''), 'LAIN-LAIN') ORDER BY COALESCE(SUM(s.grandTotal), 0) DESC")
        List<Object[]> sumGrandTotalByKategoriNama();

        /**
         * Agregasi: group by kategori_itemcode & kategori_nama untuk hierarki.
         */
        @Query("SELECT s.kategoriItemcode, s.kategoriNama, COALESCE(SUM(s.grandTotal), 0) FROM Stock s WHERE s.kategoriItemcode IS NOT NULL AND s.kategoriNama IS NOT NULL AND s.finalStok >= 1 GROUP BY s.kategoriItemcode, s.kategoriNama ORDER BY s.kategoriItemcode ASC, COALESCE(SUM(s.grandTotal), 0) DESC")
        List<Object[]> sumGrandTotalByKategoriHierarchy();

        /** Total grand_total semua stok (untuk hitung presentase). */
        @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Stock s WHERE s.finalStok >= 1")
        java.math.BigDecimal sumAllGrandTotal();
}