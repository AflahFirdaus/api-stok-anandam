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
                        "(:categories IS NULL OR s.kategoriItemcode IN :categories) AND "
                        +
                        "(:warehouse IS NULL OR :warehouse = '' OR LOWER(COALESCE(s.warehouse, '')) LIKE LOWER(CONCAT('%', :warehouse, '%'))) AND "
                        +
                        "s.finalStok >= 1")
        Page<Stock> findByFilters(@Param("search") String search, @Param("categories") List<String> categories,
                        @Param("warehouse") String warehouse, Pageable pageable);

        @Query(value = "SELECT s.itemCode FROM Stock s WHERE " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(TRIM(s.itemName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(TRIM(s.itemCode)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(TRIM(COALESCE(s.kategoriItemcode, ''))) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(TRIM(COALESCE(s.kategoriNama, ''))) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                        +
                        "(:categories IS NULL OR s.kategoriItemcode IN :categories OR s.kategoriNama IN :categories OR " +
                        "('LCD' IN :categories AND (LOWER(TRIM(s.itemCode)) LIKE 'lcd%' OR LOWER(TRIM(s.kategoriItemcode)) LIKE '%mon%' OR LOWER(TRIM(s.kategoriNama)) LIKE '%monitor%'))) AND "
                        +
                        "s.finalStok >= 1 " +
                        "GROUP BY s.itemCode", countQuery = "SELECT count(DISTINCT s.itemCode) FROM Stock s WHERE " +
                                        "(:search IS NULL OR :search = '' OR " +
                                        "LOWER(TRIM(s.itemName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "LOWER(TRIM(s.itemCode)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "LOWER(TRIM(COALESCE(s.kategoriItemcode, ''))) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "LOWER(TRIM(COALESCE(s.kategoriNama, ''))) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                                        +
                                        "(:categories IS NULL OR s.kategoriItemcode IN :categories OR s.kategoriNama IN :categories OR " +
                                        "('LCD' IN :categories AND (LOWER(TRIM(s.itemCode)) LIKE 'lcd%' OR LOWER(TRIM(s.kategoriItemcode)) LIKE '%mon%' OR LOWER(TRIM(s.kategoriNama)) LIKE '%monitor%'))) AND "
                                        +
                                        "s.finalStok >= 1")
        Page<String> findDistinctItemCodes(@Param("search") String search, @Param("categories") List<String> categories,
                        Pageable pageable);

        /** Fetch unique item codes with joined sorting. */
        @Query(value = "SELECT s.itemCode FROM Stock s LEFT JOIN Pricelist p ON s.normalizedItemName = p.normalizedItemName WHERE " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(TRIM(s.itemName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(TRIM(s.itemCode)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(TRIM(COALESCE(s.kategoriItemcode, ''))) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(TRIM(COALESCE(s.kategoriNama, ''))) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                        +
                        "(:categories IS NULL OR s.kategoriItemcode IN :categories OR s.kategoriNama IN :categories OR " +
                        "('LCD' IN :categories AND (LOWER(TRIM(s.itemCode)) LIKE 'lcd%' OR LOWER(TRIM(s.kategoriItemcode)) LIKE '%mon%' OR LOWER(TRIM(s.kategoriNama)) LIKE '%monitor%'))) AND "
                        +
                        "s.finalStok >= 1 " +
                        "GROUP BY s.itemCode, p.modal, p.finalPricelist, p.spesifikasi " +
                        "ORDER BY " +
                        "CASE WHEN :direction = 'asc' AND :sortBy = 's.itemName' THEN MIN(s.itemName) END ASC, " +
                        "CASE WHEN :direction = 'asc' AND :sortBy = 'p.modal' THEN p.modal END ASC, " +
                        "CASE WHEN :direction = 'asc' AND :sortBy = 'p.finalPricelist' THEN p.finalPricelist END ASC, "
                        +
                        "CASE WHEN :direction = 'asc' AND :sortBy = 'p.spesifikasi' THEN p.spesifikasi END ASC, " +
                        "CASE WHEN :direction = 'asc' AND :sortBy = 'SUM(s.finalStok)' THEN SUM(s.finalStok) END ASC, "
                        +
                        "CASE WHEN :direction = 'desc' AND :sortBy = 's.itemName' THEN MIN(s.itemName) END DESC, " +
                        "CASE WHEN :direction = 'desc' AND :sortBy = 'p.modal' THEN p.modal END DESC, " +
                        "CASE WHEN :direction = 'desc' AND :sortBy = 'p.finalPricelist' THEN p.finalPricelist END DESC, "
                        +
                        "CASE WHEN :direction = 'desc' AND :sortBy = 'p.spesifikasi' THEN p.spesifikasi END DESC, " +
                        "CASE WHEN :direction = 'desc' AND :sortBy = 'SUM(s.finalStok)' THEN SUM(s.finalStok) END DESC", countQuery = "SELECT count(DISTINCT s.itemCode) FROM Stock s LEFT JOIN Pricelist p ON s.normalizedItemName = p.normalizedItemName WHERE "
                                        +
                                        "(:search IS NULL OR :search = '' OR " +
                                        "LOWER(TRIM(s.itemName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "LOWER(TRIM(s.itemCode)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "LOWER(TRIM(COALESCE(s.kategoriItemcode, ''))) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "LOWER(TRIM(COALESCE(s.kategoriNama, ''))) LIKE LOWER(CONCAT('%', :search, '%'))) AND "
                                        +
                                        "(:categories IS NULL OR s.kategoriItemcode IN :categories OR s.kategoriNama IN :categories OR " +
                                        "('LCD' IN :categories AND (LOWER(TRIM(s.itemCode)) LIKE 'lcd%' OR LOWER(TRIM(s.kategoriItemcode)) LIKE '%mon%' OR LOWER(TRIM(s.kategoriNama)) LIKE '%monitor%'))) AND "
                                        +
                                        "s.finalStok >= 1")
        Page<String> findDistinctItemCodesSortedByPricelist(@Param("search") String search,
                        @Param("categories") List<String> categories,
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

        // ─── STOK PER BADAN ────────────────────────────────────────────────

        /**
         * Query stok per badan: hitung total pembelian dikurangi total penjualan
         * per kombinasi (badan, dep_code, item_code). Ekstraksi badan dari
         * par_name (purchases) / code (sales) menggunakan regex word boundary
         * case-insensitive.
         *
         * Mapping result: [0]badan, [1]dep_code, [2]dep_name, [3]item_code,
         * [4]item_name, [5]stok_qty(int), [6]line_count(int)
         */
        @Query(value = """
                WITH pur AS (
                  SELECT p.id, p.par_name, p.doc_no_p, p.doc_date, p.dep_code, p.dep_name,
                        p.item_code, p.item_name, p.qty, p.last_synced,
                        CASE
                          WHEN p.par_name ~* '\\mSGI\\M' THEN 'SGI'
                          WHEN p.par_name ~* '\\mSSS\\M' THEN 'SSS'
                          WHEN p.par_name ~* '\\mGBH\\M' THEN 'GBH'
                          WHEN p.par_name ~* '\\mMGC\\M' THEN 'MGC'
                          WHEN p.par_name ~* '\\mPDB\\M' THEN 'PDB'
                          WHEN p.par_name ~* '\\mANC\\M' THEN 'ANC'
                          ELSE 'ANC'
                        END AS badan
                  FROM purchases p
                ),
                sls AS (
                  SELECT s.ite_code, s.dep_code, s.dep_name, s.item_name, s.qty,
                        CASE
                          WHEN s.code ~* '\\mSGI\\M' THEN 'SGI'
                          WHEN s.code ~* '\\mSSS\\M' THEN 'SSS'
                          WHEN s.code ~* '\\mGBH\\M' THEN 'GBH'
                          WHEN s.code ~* '\\mMGC\\M' THEN 'MGC'
                          WHEN s.code ~* '\\mPDB\\M' THEN 'PDB'
                          WHEN s.code ~* '\\mANC\\M' THEN 'ANC'
                          ELSE 'ANC'
                        END AS badan
                  FROM sales s
                ),
                pur_agg AS (
                  SELECT badan, dep_code, dep_name, item_code, item_name,
                         SUM(qty) AS total_pur_qty,
                         COUNT(*) AS purchase_lines
                  FROM pur
                  GROUP BY badan, dep_code, dep_name, item_code, item_name
                ),
                sls_agg AS (
                  SELECT badan, dep_code, ite_code,
                         MIN(dep_name) AS dep_name,
                         MIN(item_name) AS item_name,
                         SUM(qty) AS total_sls_qty
                  FROM sls
                  GROUP BY badan, dep_code, ite_code
                )
                SELECT
                  COALESCE(pa.badan, sa.badan) AS badan,
                  COALESCE(pa.dep_code, sa.dep_code) AS dep_code,
                  COALESCE(pa.dep_name, sa.dep_name) AS dep_name,
                  COALESCE(pa.item_code, sa.ite_code) AS item_code,
                  COALESCE(pa.item_name, sa.item_name) AS item_name,
                  (COALESCE(pa.total_pur_qty, 0) - COALESCE(sa.total_sls_qty, 0))::int AS stok_qty,
                  COALESCE(pa.purchase_lines, 0)::int AS line_count
                FROM pur_agg pa
                FULL OUTER JOIN sls_agg sa
                  ON sa.badan = pa.badan
                 AND sa.dep_code = pa.dep_code
                 AND sa.ite_code = pa.item_code
                WHERE (COALESCE(pa.total_pur_qty, 0) - COALESCE(sa.total_sls_qty, 0)) <> 0
                ORDER BY badan, stok_qty DESC, item_name ASC
                """, nativeQuery = true)
        List<Object[]> findStokPerBadanRaw();
}