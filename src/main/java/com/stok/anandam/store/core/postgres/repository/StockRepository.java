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
    Page<Stock> findByItemNameContainingIgnoreCaseOrItemCodeContainingIgnoreCase(String itemName, String itemCode, Pageable pageable);

    /** Filter opsional: search (nama/code), kategori (kategori_itemcode), warehouse. Semua AND, contains/equals ignore case. */
    @Query("SELECT s FROM Stock s WHERE " +
            "(:search IS NULL OR :search = '' OR LOWER(s.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.itemCode) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:kategori IS NULL OR :kategori = '' OR LOWER(TRIM(COALESCE(s.kategoriItemcode, ''))) = LOWER(TRIM(:kategori)) OR LOWER(s.kategoriItemcode) LIKE LOWER(CONCAT('%', :kategori, '%'))) AND " +
            "(:warehouse IS NULL OR :warehouse = '' OR LOWER(COALESCE(s.warehouse, '')) LIKE LOWER(CONCAT('%', :warehouse, '%')))")
    Page<Stock> findByFilters(@Param("search") String search, @Param("kategori") String kategori, @Param("warehouse") String warehouse, Pageable pageable);

    long countByFinalStokLessThan(Integer threshold);
    List<Stock> findTop5ByFinalStokLessThanOrderByFinalStokAsc(Integer threshold);

    /** Agregasi: group by kategori_itemcode, sum(grand_total). Urut nilai terbesar dulu. */
    @Query("SELECT s.kategoriItemcode, COALESCE(SUM(s.grandTotal), 0) FROM Stock s WHERE s.kategoriItemcode IS NOT NULL AND TRIM(s.kategoriItemcode) <> '' GROUP BY s.kategoriItemcode ORDER BY COALESCE(SUM(s.grandTotal), 0) DESC")
    List<Object[]> sumGrandTotalByKategoriItemcode();

    /** Agregasi: group by kategori_nama, sum(grand_total). Urut nilai terbesar dulu. */
    @Query("SELECT s.kategoriNama, COALESCE(SUM(s.grandTotal), 0) FROM Stock s WHERE s.kategoriNama IS NOT NULL AND TRIM(s.kategoriNama) <> '' GROUP BY s.kategoriNama ORDER BY COALESCE(SUM(s.grandTotal), 0) DESC")
    List<Object[]> sumGrandTotalByKategoriNama();

    /** Total grand_total semua stok (untuk hitung presentase). */
    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Stock s")
    java.math.BigDecimal sumAllGrandTotal();
}